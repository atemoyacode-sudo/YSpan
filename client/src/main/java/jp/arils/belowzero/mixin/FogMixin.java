package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;
@Mixin(FogRenderer.class)
public abstract class FogMixin {
 @ModifyVariable(method="setupColor",at=@At(value="STORE",ordinal=0),ordinal=0) private static double bz$voidFog(double value){return BridgeState.active?Math.max(1.0,value):value;}
}
