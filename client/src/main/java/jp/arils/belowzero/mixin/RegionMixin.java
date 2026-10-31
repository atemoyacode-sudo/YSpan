package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;import net.minecraft.core.BlockPos;import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderChunkRegion.class)
public abstract class RegionMixin {
 @Inject(method="isAllEmpty",at=@At("HEAD"),cancellable=true) private static void bz$empty(BlockPos min,BlockPos max,int originX,int originZ,LevelChunk[][] chunks,CallbackInfoReturnable<Boolean> cir){
  if(!BridgeState.active||(min.getY()>=0&&max.getY()<256))return;
  for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++)for(int sy=Math.floorDiv(min.getY(),16);sy<=Math.floorDiv(max.getY(),16);sy++){
   if(sy<0||sy>15){if(BridgeState.nonempty(x,sy,z)){cir.setReturnValue(false);return;}}
   else if(!LevelChunkSection.isEmpty(chunks[x-originX][z-originZ].getSections()[sy])){cir.setReturnValue(false);return;}
  }
  cir.setReturnValue(true);
 }
}
