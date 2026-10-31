package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.client.renderer.LevelRenderer;import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;
@Mixin(LevelRenderer.class)
public abstract class RendererMixin {
 @Redirect(method="getRelativeFrom",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;getY()I")) private int bz$relativeY(BlockPos p){return p.getY()-(BridgeState.active?BridgeState.minY:0);}
 @ModifyConstant(method="getRelativeFrom",constant=@Constant(intValue=256)) private int bz$height(int value){return BridgeState.active?BridgeState.maxY-BridgeState.minY:value;}
}
