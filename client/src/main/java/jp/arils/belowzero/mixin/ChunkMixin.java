package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.chunk.LevelChunk;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class ChunkMixin {
 @Inject(method="getBlockState",at=@At("HEAD"),cancellable=true) private void bz$state(BlockPos p,CallbackInfoReturnable<BlockState> cir){if(BridgeState.outside(p.getY()))cir.setReturnValue(BridgeState.state(p.getX(),p.getY(),p.getZ()));}
 @Inject(method="getFluidState(III)Lnet/minecraft/world/level/material/FluidState;",at=@At("HEAD"),cancellable=true) private void bz$fluid(int x,int y,int z,CallbackInfoReturnable<FluidState> cir){if(BridgeState.outside(y))cir.setReturnValue(BridgeState.state(x,y,z).getFluidState());}
 @Inject(method="setBlockState",at=@At("HEAD"),cancellable=true) private void bz$set(BlockPos p,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> cir){if(BridgeState.outside(p.getY()))cir.setReturnValue(BridgeState.put(p,state));}
}
