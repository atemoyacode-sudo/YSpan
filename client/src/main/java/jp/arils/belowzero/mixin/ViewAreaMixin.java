package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {
 @Shadow protected int chunkGridSizeY;
 @Shadow protected int chunkGridSizeX;
 @Shadow protected int chunkGridSizeZ;
 @Shadow public ChunkRenderDispatcher.RenderChunk[] chunks;
 @ModifyConstant(method="setViewDistance",constant=@Constant(intValue=16))
 private int bz$count(int value){return BridgeState.active?(BridgeState.maxY-BridgeState.minY)/16:value;}
 @Redirect(method={"createChunks","repositionCamera"},at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;setOrigin(III)V"))
 private void bz$origin(ChunkRenderDispatcher.RenderChunk chunk,int x,int y,int z){chunk.setOrigin(x,y+(BridgeState.active?BridgeState.minY:0),z);}
 @Inject(method="getRenderChunkAt",at=@At("HEAD"),cancellable=true)
 private void bz$chunk(BlockPos pos,CallbackInfoReturnable<ChunkRenderDispatcher.RenderChunk> cir){
  if(!BridgeState.active)return;
  int y=Math.floorDiv(pos.getY()-BridgeState.minY,16);
  if(y<0||y>=chunkGridSizeY){cir.setReturnValue(null);return;}
  int x=Math.floorMod(Math.floorDiv(pos.getX(),16),chunkGridSizeX),z=Math.floorMod(Math.floorDiv(pos.getZ(),16),chunkGridSizeZ);
  cir.setReturnValue(chunks[(z*chunkGridSizeY+y)*chunkGridSizeX+x]);
 }
 @Inject(method="setDirty",at=@At("HEAD"),cancellable=true)
 private void bz$dirty(int x,int y,int z,boolean important,CallbackInfo ci){
  if(!BridgeState.active)return;
  ci.cancel();int yy=y-BridgeState.minY/16;
  if(yy<0||yy>=chunkGridSizeY)return;
  int xx=Math.floorMod(x,chunkGridSizeX),zz=Math.floorMod(z,chunkGridSizeZ);
  chunks[(zz*chunkGridSizeY+yy)*chunkGridSizeX+xx].setDirty(important);
 }
}
