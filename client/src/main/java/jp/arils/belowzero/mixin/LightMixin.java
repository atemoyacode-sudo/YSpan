package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.world.level.BlockAndTintGetter;import net.minecraft.world.level.LightLayer;import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockAndTintGetter.class)
public interface LightMixin {
 @Inject(method="getBrightness",at=@At("HEAD"),cancellable=true) default void bz$light(LightLayer layer,BlockPos p,CallbackInfoReturnable<Integer> cir){if(BridgeState.outside(p.getY()))cir.setReturnValue(BridgeState.light(layer,p));}
 @Inject(method="getRawBrightness",at=@At("HEAD"),cancellable=true) default void bz$raw(BlockPos p,int skyDark,CallbackInfoReturnable<Integer> cir){if(BridgeState.outside(p.getY()))cir.setReturnValue(Math.max(BridgeState.light(LightLayer.BLOCK,p),BridgeState.light(LightLayer.SKY,p)-skyDark));}
}
