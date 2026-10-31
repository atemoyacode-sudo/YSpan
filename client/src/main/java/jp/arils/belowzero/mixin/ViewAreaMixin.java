package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.client.renderer.ViewArea;import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;
@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {
 @Shadow protected int chunkGridSizeY;@Shadow protected int chunkGridSizeX;@Shadow protected int chunkGridSizeZ;@Shadow public ChunkRenderDispatcher.RenderChunk[] chunks;
 @ModifyConstant(method="setViewDistance",constant=@Constant(intValue=16)) private int bz$count(int value){return BridgeState.active?(BridgeState.maxY-BridgeState.minY)/16:value;}
 @Redirect(method={"createChunks","repositionCamera"},at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;setOrigin(III)V")) private void bz$origin(ChunkRenderDispatcher.RenderChunk c,int x,int y,int z){c.setOrigin(x,y+(BridgeState.active?BridgeState.minY:0),z);}
 /** @author Arils @reason Add explicit vertical origin to the client render grid. */
 @Overwrite public ChunkRenderDispatcher.RenderChunk getRenderChunkAt(BlockPos p){int y=Math.floorDiv(p.getY()-(BridgeState.active?BridgeState.minY:0),16);if(y<0||y>=chunkGridSizeY)return null;int x=Math.floorMod(Math.floorDiv(p.getX(),16),chunkGridSizeX),z=Math.floorMod(Math.floorDiv(p.getZ(),16),chunkGridSizeZ);return chunks[(z*chunkGridSizeY+y)*chunkGridSizeX+x];}
 /** @author Arils @reason Keep dirty-section indices consistent with the shifted grid origin. */
 @Overwrite public void setDirty(int x,int y,int z,boolean important){int yy=y-(BridgeState.active?BridgeState.minY/16:0);if(yy<0||yy>=chunkGridSizeY)return;int xx=Math.floorMod(x,chunkGridSizeX),zz=Math.floorMod(z,chunkGridSizeZ);chunks[(zz*chunkGridSizeY+yy)*chunkGridSizeX+xx].setDirty(important);}
}
