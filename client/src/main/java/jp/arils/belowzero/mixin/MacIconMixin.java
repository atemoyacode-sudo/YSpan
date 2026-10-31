package jp.arils.belowzero.mixin;
import java.io.InputStream;import java.util.Locale;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Older Minecraft calls a GLFW operation that modern Cocoa does not support. */
@Mixin(Window.class)
public abstract class MacIconMixin {
 @Inject(method="setIcon",at=@At("HEAD"),cancellable=true) private void bz$macIcon(InputStream a,InputStream b,CallbackInfo ci){if(System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("mac"))ci.cancel();}
}
