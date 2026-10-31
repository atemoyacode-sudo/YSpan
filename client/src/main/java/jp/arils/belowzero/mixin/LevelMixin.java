package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class LevelMixin {
 @Inject(method="isOutsideBuildHeight(I)Z",at=@At("HEAD"),cancellable=true) private static void bz$bounds(int y,CallbackInfoReturnable<Boolean> cir){if(BridgeState.active)cir.setReturnValue(y<BridgeState.minY||y>=BridgeState.maxY);}
}
