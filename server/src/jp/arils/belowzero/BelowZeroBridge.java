package jp.arils.belowzero;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

/** Experimental supplemental chunk channel; no world edits unless the loopback-only fixture is explicitly enabled. */
public final class BelowZeroBridge extends JavaPlugin implements Listener, PluginMessageListener {
 static final String CHANNEL="arils:belowzero";
 static final int MAGIC=0x425a4231;
 final Map<UUID,Session> sessions=new HashMap<>();
 final Map<String,Integer> blockIds=new HashMap<>();
 Method stateMethod,idMethod;
 long tick;
 boolean fixture;
 record Key(int x,int y,int z){}
 static final class Session {
  long epoch; UUID world; final ArrayDeque<Key> pending=new ArrayDeque<>(); final Map<Key,Integer> sent=new HashMap<>();
 }
 @Override public void onEnable(){
  saveDefaultConfig();fixture=getConfig().getBoolean("test-fixture",false);
  if(fixture && (!"127.0.0.1".equals(getServer().getIp()) || !getServer().getWorlds().getFirst().getName().equals("bz-test")))throw new IllegalStateException("Fixture requires explicit loopback bz-test server");
  getServer().getMessenger().registerIncomingPluginChannel(this,CHANNEL,this);
  getServer().getMessenger().registerOutgoingPluginChannel(this,CHANNEL);
  getServer().getPluginManager().registerEvents(this,this);
  getServer().getScheduler().runTaskTimer(this,this::stream,1,1);
  if(fixture){createFixture(getServer().getWorlds().getFirst());getLogger().info("LOCAL_FIXTURE_READY");}
 }
 @Override public void onPluginMessageReceived(String channel,Player p,byte[] bytes){
  if(!CHANNEL.equals(channel)||bytes.length<5||bytes.length>512)return;
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
   if(in.readInt()!=MAGIC)return;int kind=in.readUnsignedByte();
   if(kind==0){
    if(in.readInt()!=1 || in.available()!=0 || Via.getAPI().getPlayerVersion(p.getUniqueId())!=754)return;
    Session s=new Session();sessions.put(p.getUniqueId(),s);hello(p,s);
    getLogger().info("BRIDGE_HANDSHAKE player="+p.getName()+" protocol=754 world="+p.getWorld().getName());
   }else if(kind==3 && fixture && p.getName().equals("BZProbe") && p.getAddress().getAddress().isLoopbackAddress()){
    String stage=in.readUTF();if(!stage.matches("[a-z0-9_-]{1,40}"))return;
    switch(stage){
     case "goto-portal" -> {
      World w=getServer().getWorlds().getFirst();for(int x=7;x<=9;x++)for(int z=11;z<=13;z++){w.getBlockAt(x,-3,z).setType(Material.END_PORTAL,false);w.getBlockAt(x,-2,z).setType(Material.AIR,false);}
      for(Session session:sessions.values()){session.sent.clear();session.pending.clear();}
      p.teleport(new Location(w,8.5,-2,8.5,0,20));
     }
     case "goto-bottom" -> arena(p,getServer().getWorlds().getFirst(),40,-63);
     case "goto-seam" -> arena(p,getServer().getWorlds().getFirst(),72,0);
     case "goto-upper" -> arena(p,getServer().getWorlds().getFirst(),104,256);
     case "goto-top" -> arena(p,getServer().getWorlds().getFirst(),136,318);
     case "goto-end" -> {World end=getServer().getWorlds().stream().filter(w->w.getEnvironment()==World.Environment.THE_END).findFirst().orElseThrow();arena(p,end,8,8);}
     case "goto-return" -> {p.teleport(new Location(getServer().getWorlds().getFirst(),8.5,-2,8.5,0,10));}
    }
    Location a=p.getLocation();
    java.nio.file.Files.writeString(getDataFolder().toPath().resolve("probe-records.jsonl"),"{\"time\":\""+java.time.Instant.now()+"\",\"stage\":\""+stage+"\",\"x\":"+a.getX()+",\"y\":"+a.getY()+",\"z\":"+a.getZ()+",\"target\":\""+p.getWorld().getBlockAt(8,-2,11).getType()+"\",\"onGround\":"+p.isOnGround()+",\"world\":\""+p.getWorld().getKey()+"\"}\n",java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
    getLogger().info("PROBE stage="+stage+" pos="+a.getX()+","+a.getY()+","+a.getZ()+" target="+p.getWorld().getBlockAt(8,-2,11).getType()+" onGround="+p.isOnGround());
    ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeInt(MAGIC);out.writeByte(4);out.writeUTF(stage);out.writeDouble(a.getX());out.writeDouble(a.getY());out.writeDouble(a.getZ());out.writeUTF(p.getWorld().getBlockAt(8,-2,11).getType().name());out.writeBoolean(p.isOnGround());p.sendPluginMessage(this,CHANNEL,b.toByteArray());
   }
  }catch(Exception e){getLogger().warning("Bridge request failed: "+e.getClass().getSimpleName());}
 }
 void hello(Player p,Session s)throws IOException{
  s.epoch=System.nanoTime();s.world=p.getWorld().getUID();s.sent.clear();s.pending.clear();
  World w=p.getWorld();ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);
  out.writeInt(MAGIC);out.writeByte(1);out.writeLong(s.epoch);out.writeUTF(w.getKey().toString());out.writeInt(w.getMinHeight());out.writeInt(w.getMaxHeight());p.sendPluginMessage(this,CHANNEL,b.toByteArray());
 }
 void stream(){
  tick++;
  for(var it=sessions.entrySet().iterator();it.hasNext();){
   var e=it.next();Player p=getServer().getPlayer(e.getKey());Session s=e.getValue();if(p==null){it.remove();continue;}
   try{
    if(!p.getWorld().getUID().equals(s.world))hello(p,s);
    World w=p.getWorld();int cx=p.getLocation().getBlockX()>>4,cz=p.getLocation().getBlockZ()>>4;
    int radius=Math.max(1,Math.min(4,getConfig().getInt("stream-radius",2)));
    s.sent.keySet().removeIf(k->Math.abs(k.x-cx)>radius+1||Math.abs(k.z-cz)>radius+1);
    if(s.pending.isEmpty() && tick%10==0){
     for(int ring=0;ring<=radius;ring++)for(int x=cx-ring;x<=cx+ring;x++)for(int z=cz-ring;z<=cz+ring;z++){
      if(Math.max(Math.abs(x-cx),Math.abs(z-cz))!=ring||!w.isChunkLoaded(x,z))continue;
      for(int y=Math.floorDiv(w.getMinHeight(),16);y<Math.floorDiv(w.getMaxHeight(),16);y++)if(y<0||y>15)s.pending.add(new Key(x,y,z));
     }
    }
    for(int n=0;n<Math.max(1,Math.min(8,getConfig().getInt("sections-per-tick",2)))&&!s.pending.isEmpty();n++){
     Key k=s.pending.poll();if(Math.abs(k.x-cx)>radius||Math.abs(k.z-cz)>radius||!w.isChunkLoaded(k.x,k.z))continue;
     ChunkSnapshot snap=w.getChunkAt(k.x,k.z).getChunkSnapshot(false,false,false);
     ByteArrayOutputStream raw=new ByteArrayOutputStream(16384);DataOutputStream data=new DataOutputStream(raw);
     for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
      int realY=k.y*16+y;int id=legacyId(snap.getBlockData(x,realY,z));
      data.writeInt((id<<8)|(snap.getBlockSkyLight(x,realY,z)<<4)|snap.getBlockEmittedLight(x,realY,z));
     }
     byte[] values=raw.toByteArray();int hash=Arrays.hashCode(values);if(Objects.equals(s.sent.get(k),hash))continue;
     ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(DeflaterOutputStream zip=new DeflaterOutputStream(compressed)){zip.write(values);}
     if(compressed.size()>20000)throw new IOException("Section exceeded size bound");
     ByteArrayOutputStream body=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(body);
     out.writeInt(MAGIC);out.writeByte(2);out.writeLong(s.epoch);out.writeInt(k.x);out.writeInt(k.y);out.writeInt(k.z);out.writeInt(compressed.size());out.write(compressed.toByteArray());p.sendPluginMessage(this,CHANNEL,body.toByteArray());s.sent.put(k,hash);
    }
   }catch(Exception ex){getLogger().warning("Bridge stream stopped for "+p.getName()+": "+ex);it.remove();}
  }
 }
 int legacyId(BlockData data)throws Exception{
  Integer cached=blockIds.get(data.getAsString());if(cached!=null)return cached;
  if(stateMethod==null){stateMethod=data.getClass().getMethod("getState");Class<?> state=Class.forName("net.minecraft.world.level.block.state.BlockState");idMethod=Class.forName("net.minecraft.world.level.block.Block").getMethod("getId",state);}
  int id=(int)idMethod.invoke(null,stateMethod.invoke(data));
  var server=Via.getAPI().getServerVersion().highestSupportedProtocolVersion();
  List<ProtocolPathEntry> path=Via.getManager().getProtocolManager().getProtocolPath(ProtocolVersion.v1_16_4,server);
  if(path==null)throw new IllegalStateException("Missing protocol path");
  for(int i=path.size()-1;i>=0;i--){var mappings=path.get(i).protocol().getMappingData();if(mappings!=null&&mappings.getBlockStateMappings()!=null)id=mappings.getNewBlockStateId(id);}
  if(id<0||id>65535)throw new IllegalStateException("Invalid mapped block state");blockIds.put(data.getAsString(),id);return id;
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void portal(PlayerPortalEvent e){if(fixture&&e.getPlayer().getName().equals("BZProbe"))getLogger().info("REAL_PORTAL cause="+e.getCause()+" fromY="+e.getFrom().getY()+" fromWorld="+e.getFrom().getWorld().getKey()+" toWorld="+(e.getTo()==null?"null":e.getTo().getWorld().getKey()));}
 @EventHandler public void leave(PlayerQuitEvent e){sessions.remove(e.getPlayer().getUniqueId());}
 @EventHandler public void join(PlayerJoinEvent e){if(fixture && e.getPlayer().getName().equals("BZProbe"))getServer().getScheduler().runTaskLater(this,()->{Player p=e.getPlayer();p.setGameMode(GameMode.CREATIVE);p.setFlying(false);p.teleport(new Location(getServer().getWorlds().getFirst(),8.5,-2,8.5,0,10));p.getInventory().setItem(0,new ItemStack(Material.COBBLESTONE,64));},10);}
 void dirty(org.bukkit.block.Block b){Key key=new Key(b.getX()>>4,b.getY()>>4,b.getZ()>>4);if(key.y>=0&&key.y<=15)return;for(Session s:sessions.values())if(s.world.equals(b.getWorld().getUID())){s.sent.remove(key);s.pending.addFirst(key);}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent e){getServer().getScheduler().runTask(this,()->dirty(e.getBlock()));}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placed(BlockPlaceEvent e){getServer().getScheduler().runTask(this,()->dirty(e.getBlock()));}
 void arena(Player p,World w,int cx,int feet){
  for(int x=cx-4;x<=cx+4;x++)for(int z=4;z<=12;z++){
   w.getBlockAt(x,feet-1,z).setType(Material.STONE,false);
   for(int y=feet;y<=Math.min(feet+2,w.getMaxHeight()-1);y++)w.getBlockAt(x,y,z).setType(x==cx-4||x==cx+4||z==4||z==12?Material.GLASS:Material.AIR,false);
  }
  w.getBlockAt(cx-2,feet-1,6).setType(Material.SEA_LANTERN,false);
  p.teleport(new Location(w,cx+.5,feet,8.5,0,20));
 }
 void createFixture(World w){
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   w.getBlockAt(x,-3,z).setType(Material.STONE,false);
   for(int y=-2;y<=2;y++)w.getBlockAt(x,y,z).setType((x==0||x==15||z==0||z==15)?Material.GLASS:Material.AIR,false);
  }
  w.getBlockAt(8,-2,11).setType(Material.GOLD_BLOCK,false);
  w.getBlockAt(6,-2,6).setType(Material.SEA_LANTERN,false);
  w.setSpawnLocation(8,1,8);w.setTime(6000);
 }
}
