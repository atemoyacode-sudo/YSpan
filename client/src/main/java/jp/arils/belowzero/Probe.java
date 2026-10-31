package jp.arils.belowzero;
import java.io.*;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;import net.minecraft.client.Screenshot;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.core.BlockPos;import net.minecraft.core.Direction;import net.minecraft.world.InteractionHand;import net.minecraft.world.phys.*;
public final class Probe {
 static int readyTicks,menuTicks,retries,boundaryIndex,boundaryTick;static boolean boundaryRequested;static int portalTick;static boolean portalSent,portalWalking,controlDone;
 static final String[] BOUNDARIES={"bottom","seam","upper","top","end","return"};
 static final int[] BOUNDARY_X={40,72,104,136,8,8},BOUNDARY_Y={-63,0,256,318,8,-2};static boolean started,connected;
 public static void tick(){
  if(!Boolean.getBoolean("belowzero.probe"))return;
  Minecraft mc=Minecraft.getInstance();
  if(Boolean.getBoolean("belowzero.disabled")&&mc.player!=null&&mc.level!=null){
   menuTicks++;
   if(menuTicks==100&&!controlDone){controlDone=true;System.out.println("[BelowZero PROBE] CONTROL inactive floor="+mc.level.getBlockState(new BlockPos(8,-3,8))+" playerY="+mc.player.getY());try{report("control-floor");}catch(IOException e){throw new RuntimeException(e);}mc.options.renderDebug=true;mc.player.xRot=30;Screenshot.grab(mc.gameDirectory,"control-disabled.png",mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getMainRenderTarget(),c->{});}
   return;
  }
  if(!BridgeState.active||mc.player==null||mc.level==null){
   menuTicks++;
   if(menuTicks%100==0)System.out.println("[BelowZero PROBE] screen="+(mc.screen==null?"none":mc.screen.getClass().getName())+" overlay="+(mc.getOverlay()==null?"none":mc.getOverlay().getClass().getName()));
   if(menuTicks==120)Screenshot.grab(mc.gameDirectory,"startup-proof.png",mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getMainRenderTarget(),c->System.out.println("[BelowZero PROBE] "+c.getString()));
   if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen && menuTicks%200==0 && retries++<3){mc.setScreen(new net.minecraft.client.gui.screens.ConnectScreen(mc.screen,mc,"127.0.0.1",25576));}
   if(!connected&&menuTicks>40&&mc.getOverlay()==null&&mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen){connected=true;mc.setScreen(new net.minecraft.client.gui.screens.ConnectScreen(mc.screen,mc,"127.0.0.1",25576));}
   return;
  }
  if(!mc.player.getName().getString().equals("BZProbe"))return;
  if(readyTicks==0&&!mc.level.getBlockState(new BlockPos(8,-3,8)).getMaterial().isSolid())return;
  readyTicks++;
  try{
   if(readyTicks==40){started=true;report("floor-ready");System.out.println("[BelowZero PROBE] floor="+mc.level.getBlockState(new BlockPos(8,-3,8))+" y="+mc.player.getY()+" received="+BridgeState.received);mc.options.hideGui=false;mc.options.autoJump=false;mc.options.renderDebug=true;mc.player.yRot=0;mc.player.xRot=20;}
   if(readyTicks==60)mc.options.keyLeft.setDown(true);
   if(readyTicks==70){mc.options.keyLeft.setDown(false);report("walked");}
   if(readyTicks==90){mc.gameMode.startDestroyBlock(new BlockPos(8,-2,11),Direction.NORTH);System.out.println("[BelowZero PROBE] sent native break below zero");}
   if(readyTicks==120)report("broken");
   if(readyTicks==140){mc.player.inventory.selected=0;mc.gameMode.useItemOn(mc.player,mc.level,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(8.5,-2,11.5),Direction.UP,new BlockPos(8,-3,11),false));System.out.println("[BelowZero PROBE] sent native place below zero");}
   if(readyTicks==175){report("placed");System.out.println("[BelowZero PROBE] client placed="+mc.level.getBlockState(new BlockPos(8,-2,11)));double dx=8.5-mc.player.getX(),dz=11.5-mc.player.getZ();mc.player.yRot=(float)(Math.atan2(dz,dx)*180/Math.PI-90);mc.player.xRot=25;Screenshot.grab(mc.gameDirectory,"below-zero-proof.png",mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getMainRenderTarget(),c->System.out.println("[BelowZero PROBE] screenshot="+c.getString()));}
   if(readyTicks==200)System.out.println("[BelowZero PROBE] initial checks finished; boundary tests begin");
   if(readyTicks>220 && boundaryIndex<BOUNDARIES.length){
    if(!boundaryRequested){report("goto-"+BOUNDARIES[boundaryIndex]);boundaryRequested=true;boundaryTick=readyTicks;}
    else if(readyTicks-boundaryTick>=60){
     BlockPos floor=new BlockPos(BOUNDARY_X[boundaryIndex],BOUNDARY_Y[boundaryIndex]-1,8);
     boolean floorOk=mc.level.getBlockState(floor).getMaterial().isSolid();
     boolean heightOk=Math.abs(mc.player.getY()-BOUNDARY_Y[boundaryIndex])<.05;
     if(floorOk&&heightOk&&mc.player.isOnGround()){
      System.out.println("[BelowZero PROBE] BOUNDARY_PASS "+BOUNDARIES[boundaryIndex]+" floor="+mc.level.getBlockState(floor)+" playerY="+mc.player.getY()+" dimension="+mc.level.dimension().location());
      report(BOUNDARIES[boundaryIndex]+"-verified");
      if(boundaryIndex==0||boundaryIndex==3)Screenshot.grab(mc.gameDirectory,"boundary-"+BOUNDARIES[boundaryIndex]+".png",mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getMainRenderTarget(),c->{});
      boundaryIndex++;boundaryRequested=false;
     }else if(readyTicks-boundaryTick>240){System.err.println("[BelowZero PROBE] BOUNDARY_FAIL "+BOUNDARIES[boundaryIndex]+" floor="+floorOk+" y="+mc.player.getY());boundaryIndex++;boundaryRequested=false;}
    }
   }
   if(boundaryIndex==BOUNDARIES.length){boundaryIndex++;System.out.println("[BelowZero PROBE] ALL_BOUNDARY_TESTS_FINISHED");report("goto-portal");portalTick=readyTicks;portalSent=true;}
   if(portalSent){
    int elapsed=readyTicks-portalTick;
    if(elapsed>=60&&!portalWalking&&mc.level.dimension().location().toString().equals("minecraft:overworld")){
     System.out.println("[BelowZero PROBE] portal floor="+mc.level.getBlockState(new BlockPos(8,-3,12)));
     mc.player.yRot=0;mc.player.xRot=25;Screenshot.grab(mc.gameDirectory,"portal-before-entry.png",mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getMainRenderTarget(),c->{});mc.options.keyUp.setDown(true);portalWalking=true;
    }
    if(mc.level.dimension().location().toString().equals("minecraft:the_end")){
     mc.options.keyUp.setDown(false);
     if(elapsed>=100&&mc.player.isOnGround()){
      report("portal-verified");System.out.println("[BelowZero PROBE] REAL_PORTAL_PASS playerY="+mc.player.getY());Screenshot.grab(mc.gameDirectory,"portal-arrival.png",mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getMainRenderTarget(),c->{});portalSent=false;
     }
    }else if(elapsed>300){mc.options.keyUp.setDown(false);System.err.println("[BelowZero PROBE] REAL_PORTAL_FAIL y="+mc.player.getY()+" z="+mc.player.getZ());portalSent=false;}
   }
  }catch(Exception e){System.err.println("[BelowZero PROBE] "+e);}
 }
 static void report(String stage)throws IOException{Minecraft mc=Minecraft.getInstance();ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeInt(BridgeState.MAGIC);out.writeByte(3);out.writeUTF(stage);mc.getConnection().send(new ServerboundCustomPayloadPacket(BridgeState.CHANNEL,new FriendlyByteBuf(Unpooled.wrappedBuffer(b.toByteArray()))));}
 public static void reply(DataInputStream in)throws IOException {String stage=in.readUTF();double x=in.readDouble(),y=in.readDouble(),z=in.readDouble();String block=in.readUTF();boolean ground=in.readBoolean();System.out.println("[BelowZero PROBE] server stage="+stage+" pos="+x+","+y+","+z+" target="+block+" onGround="+ground);}
}
