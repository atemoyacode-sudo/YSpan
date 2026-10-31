package jp.arils.belowzero;
import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.zip.*;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LightLayer;
public final class BridgeState {
 public static final ResourceLocation CHANNEL=new ResourceLocation("arils:belowzero");
 public static final int MAGIC=0x425a4231;
 public static volatile boolean active;
 public static volatile int minY=0,maxY=256;
 public static volatile long epoch;
 public static final Map<Long,int[]> sections=new ConcurrentHashMap<>();
 public static int received;
 public static int handshakesSent;
 public static final BridgeNegotiation negotiation=new BridgeNegotiation();
 private static ClientLevel boundWorld;
 private static final Set<BlockPos> createdTiles=new HashSet<>();
 public static boolean outside(int y){return active&&(y<0||y>=256)&&y>=minY&&y<maxY;}
 public static void reset(){clearWorld(false);negotiation.disconnect();handshakesSent=0;}
 public static void connected(ClientPacketListener owner){reset();negotiation.connect(owner);}
 public static void worldChanged(ClientPacketListener owner){if(negotiation.owns(owner)){clearWorld(true);negotiation.worldChanged(owner);}}
 private static void clearWorld(boolean rebuild){
  Minecraft mc=Minecraft.getInstance();boolean hadWorld=active&&mc.level==boundWorld;
  if(hadWorld)for(BlockPos p:createdTiles)mc.level.removeBlockEntity(p);
  createdTiles.clear();active=false;minY=0;maxY=256;epoch=0;sections.clear();received=0;boundWorld=null;
  if(rebuild&&hadWorld)mc.levelRenderer.allChanged();
 }
 public static void serverChannels(ClientPacketListener owner,byte[] data,boolean register){
  Minecraft mc=Minecraft.getInstance();if(owner!=mc.getConnection()||!negotiation.owns(owner)||mc.hasSingleplayerServer())return;
  if(!negotiation.channels(owner,data,register))return;
  if(!register)clearWorld(true);else handshake(owner);
 }
 public static void tick(){if(negotiation.expire(System.nanoTime())){clearWorld(true);System.out.println("[YSpan] Companion did not respond; staying in vanilla mode");}}

 public static BlockState state(int x,int y,int z){
  int[] a=sections.get(SectionPos.asLong(x>>4,y>>4,z>>4));if(a==null)return Blocks.AIR.defaultBlockState();
  return Block.stateById(a[((y&15)<<8)|((z&15)<<4)|(x&15)]>>>8);
 }
 public static BlockState put(BlockPos p,BlockState state){
  long key=SectionPos.asLong(p.getX()>>4,p.getY()>>4,p.getZ()>>4);int[] a=sections.get(key);
  if(a==null)return Blocks.AIR.defaultBlockState();int[] copy=a.clone();int index=((p.getY()&15)<<8)|((p.getZ()&15)<<4)|(p.getX()&15);BlockState previous=Block.stateById(copy[index]>>>8);copy[index]=(Block.getId(state)<<8)|(copy[index]&255);sections.put(key,copy);return previous;
 }
 public static int light(LightLayer layer,BlockPos p){int[] a=sections.get(SectionPos.asLong(p.getX()>>4,p.getY()>>4,p.getZ()>>4));if(a==null)return 0;int v=a[((p.getY()&15)<<8)|((p.getZ()&15)<<4)|(p.getX()&15)];return layer==LightLayer.SKY?(v>>4)&15:v&15;}
 public static boolean nonempty(int x,int y,int z){int[] a=sections.get(SectionPos.asLong(x,y,z));if(a==null)return false;for(int v:a)if(!Block.stateById(v>>>8).isAir())return true;return false;}
 public static void handshake(ClientPacketListener owner){
  if(Boolean.getBoolean("belowzero.disabled"))return;
  Minecraft mc=Minecraft.getInstance();if(owner!=mc.getConnection()||mc.hasSingleplayerServer())return;
  if(!negotiation.request(owner,mc.level,System.nanoTime()))return;
  owner.send(new ServerboundCustomPayloadPacket(new ResourceLocation("minecraft:register"),new FriendlyByteBuf(Unpooled.wrappedBuffer(CHANNEL.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)))));
  FriendlyByteBuf out=new FriendlyByteBuf(Unpooled.buffer());out.writeInt(MAGIC);out.writeByte(0);out.writeInt(1);owner.send(new ServerboundCustomPayloadPacket(CHANNEL,out));handshakesSent++;
 }
 public static void receive(ClientPacketListener owner,byte[] bytes){
  Minecraft mc=Minecraft.getInstance();if(mc.level==null||owner!=mc.getConnection()||!negotiation.owns(owner)||!negotiation.advertised())return;
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
   if(in.readInt()!=MAGIC)throw new IOException("Bad magic");int kind=in.readUnsignedByte();
   if(kind==1){if(!negotiation.accepts(owner,mc.level))return;long e=in.readLong();String dimension=in.readUTF();int bottom=in.readInt(),top=in.readInt();
    if(!dimension.equals(mc.level.dimension().location().toString())||bottom< -64||top>320||bottom%16!=0||top%16!=0||top<=bottom||in.available()!=0)throw new IOException("Unsupported dimension bounds");
    clearWorld(false);negotiation.confirmed(owner,mc.level);boundWorld=mc.level;epoch=e;minY=bottom;maxY=top;active=true;mc.levelRenderer.allChanged();System.out.println("[BelowZero] ACTIVE dimension="+dimension+" range="+bottom+".."+(top-1));
   }else if(kind==2){long e=in.readLong();int x=in.readInt(),y=in.readInt(),z=in.readInt(),size=in.readInt();
    if(!active||mc.level!=boundWorld||e!=epoch)return;
    if(y*16<minY||y*16>=maxY||(y>=0&&y<=15)||size<0||size>20000||size!=in.available())throw new IOException("Invalid section bounds");
    if(mc.player!=null&&(Math.abs((long)x-(mc.player.blockPosition().getX()>>4))>8||Math.abs((long)z-(mc.player.blockPosition().getZ()>>4))>8))return;
    byte[] zip=new byte[size];in.readFully(zip);int[] values=new int[4096];
    try(DataInputStream data=new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(zip)))){for(int i=0;i<4096;i++){values[i]=data.readInt();int id=values[i]>>>8;if(id>=Block.BLOCK_STATE_REGISTRY.size())throw new IOException("Invalid state id "+id);}if(data.read()!=-1)throw new IOException("Expanded section too large");}
    int[] previous=sections.put(SectionPos.asLong(x,y,z),values);received++;
    for(int i=0;i<4096;i++){
     BlockState state=Block.stateById(values[i]>>>8);
     BlockPos pos=new BlockPos(x*16+(i&15),y*16+(i>>8),z*16+((i>>4)&15));
     if(previous!=null){BlockState old=Block.stateById(previous[i]>>>8);if(old.getBlock().isEntityBlock()&&old.getBlock()!=state.getBlock()){mc.level.removeBlockEntity(pos);createdTiles.remove(pos);}}
     if(state.getBlock().isEntityBlock()){BlockEntity tile=mc.level.getChunkAt(pos).getBlockEntity(pos);if(tile==null&&mc.level.getBlockEntity(pos)!=null)createdTiles.add(pos);}
    }
    if(mc.player!=null){final int cx=mc.player.blockPosition().getX()>>4,cz=mc.player.blockPosition().getZ()>>4;sections.keySet().removeIf(key->Math.abs(SectionPos.x(key)-cx)>8||Math.abs(SectionPos.z(key)-cz)>8);}
    mc.levelRenderer.setSectionDirty(x,y,z);
    for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)mc.levelRenderer.setSectionDirty(x+dx,y,z+dz);
    mc.levelRenderer.setSectionDirty(x,y-1,z);mc.levelRenderer.setSectionDirty(x,y+1,z);
   }else if(kind==4){Probe.reply(in);}
  }catch(Exception e){negotiation.fallback();clearWorld(true);System.err.println("[YSpan] Companion data rejected; vanilla fallback: "+e.getClass().getSimpleName());}
 }
}
