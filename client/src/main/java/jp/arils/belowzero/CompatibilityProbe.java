package jp.arils.belowzero;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Explicitly enabled, synthetic loopback-only regression scenario; disabled for normal users. */
public final class CompatibilityProbe {
 private static int phase,age,menuAge;
 private static boolean connecting,finished;
 private static double initialX,initialZ;
 private static final String[] CASES={"vanilla-first","companion","vanilla-after-companion","companion-return"};
 public static void tick(){
  if(!Boolean.getBoolean("yspan.compatProbe")||finished)return;
  Minecraft mc=Minecraft.getInstance();
  try{
   if(mc.level==null||mc.player==null){
    if(++menuAge>300)throw new IllegalStateException("Connection screen timeout");
    if(!connecting&&mc.getOverlay()==null&&mc.screen instanceof TitleScreen){connecting=true;join(mc,25577);}
    return;
   }
   if(!mc.player.getName().getString().equals("BZProbe"))throw new IllegalStateException("Synthetic account required");
   // Prevent this diagnostic from operating on an external server even when its property is set.
   java.net.SocketAddress address=mc.getConnection().getConnection().getRemoteAddress();
   if(!(address instanceof java.net.InetSocketAddress)||!((java.net.InetSocketAddress)address).getAddress().isLoopbackAddress())throw new IllegalStateException("Loopback test only");
   age++;
   boolean bridge=(phase%2)==1;
   if(age==100){
    require(mc.player.isOnGround(),"player should stand on a real floor");
    if(bridge){require(BridgeState.active&&BridgeState.received>0,"companion enables supplemental chunks");require(Math.abs(mc.player.getY()+2)<.01,"real negative Y");require(mc.level.getBlockState(new BlockPos(8,-3,8)).is(Blocks.STONE),"negative floor");}
    else require(!BridgeState.active&&BridgeState.sections.isEmpty()&&BridgeState.handshakesSent==0&&!BridgeState.negotiation.advertised()&&BridgeState.minY==0&&BridgeState.maxY==256&&BridgeState.epoch==0,"vanilla server receives no YSpan handshake or stale data");
    initialX=mc.player.getX();initialZ=mc.player.getZ();mc.options.autoJump=false;mc.player.yRot=0;mc.options.keyLeft.setDown(true);
   }
   if(age==110)mc.options.keyLeft.setDown(false);
   if(age==140){require(mc.player.isOnGround(),"walking collision");require(Math.hypot(mc.player.getX()-initialX,mc.player.getZ()-initialZ)>1,"walking must reach server-accepted terrain");}
   if(bridge&&age==150)mc.gameMode.startDestroyBlock(new BlockPos(8,-2,11),Direction.NORTH);
   if(bridge&&age==180){require(mc.level.getBlockState(new BlockPos(8,-2,11)).isAir(),"native break");mc.player.inventory.selected=0;mc.gameMode.useItemOn(mc.player,mc.level,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(8.5,-2,11.5),Direction.UP,new BlockPos(8,-3,11),false));}
   if(age==220){
    if(bridge){require(mc.level.getBlockState(new BlockPos(8,-2,11)).is(Blocks.COBBLESTONE),"native placement");Probe.report("compat-placed");}
    record(mc,"PASS",CASES[phase]);mc.player.xRot=30;
    Screenshot.grab(mc.gameDirectory,"compat-"+CASES[phase]+".png",mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getMainRenderTarget(),c->{});
   }
   if(age==240){
    phase++;age=0;menuAge=0;
    if(phase==CASES.length){disconnect(mc);require(!BridgeState.active&&BridgeState.sections.isEmpty()&&!BridgeState.negotiation.advertised()&&BridgeState.handshakesSent==0&&BridgeState.epoch==0,"final disconnect erases state");record(mc,"PASS","disconnect");finished=true;System.out.println("[YSpan COMPAT] ALL_SCENARIOS_PASS");}
    else join(mc,phase%2==1?25576:25577);
   }
  }catch(Throwable e){finished=true;mc.options.keyLeft.setDown(false);mc.options.keyUp.setDown(false);record(mc,"FAIL",e.getClass().getSimpleName()+":"+e.getMessage());System.err.println("[YSpan COMPAT] FAILED "+e);}
 }
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void disconnect(Minecraft mc){if(mc.level!=null)mc.level.disconnect();mc.clearLevel();mc.setScreen(new TitleScreen());}
 private static void join(Minecraft mc,int port){disconnect(mc);mc.setScreen(new ConnectScreen(new TitleScreen(),mc,"127.0.0.1",port));}
 private static void record(Minecraft mc,String result,String scenario){
  try{
   Map<String,Object> data=new LinkedHashMap<>();data.put("result",result);data.put("scenario",scenario);data.put("active",BridgeState.active);data.put("sections",BridgeState.sections.size());data.put("handshakes",BridgeState.handshakesSent);data.put("advertised",BridgeState.negotiation.advertised());data.put("min_y",BridgeState.minY);data.put("max_y",BridgeState.maxY);
   if(mc.player!=null){data.put("y",mc.player.getY());data.put("on_ground",mc.player.isOnGround());data.put("floor",mc.level.getBlockState(mc.player.blockPosition().below()).toString());}
   String json=new com.google.gson.Gson().toJson(data);Files.write(mc.gameDirectory.toPath().resolve("compat-results.jsonl"),(json+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);System.out.println("[YSpan COMPAT] "+json);
  }catch(Exception e){throw new RuntimeException(e);}
 }
}
