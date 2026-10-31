package jp.arils.belowzero.mixin;
import jp.arils.belowzero.Probe;import jp.arils.belowzero.BridgeState;import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class TickMixin {
 @Inject(method="clearLevel",at=@At("HEAD")) private void bz$clear(CallbackInfo ci){BridgeState.reset();}
 @Inject(method="tick",at=@At("TAIL")) private void bz$probe(CallbackInfo ci){Probe.tick();}
}
