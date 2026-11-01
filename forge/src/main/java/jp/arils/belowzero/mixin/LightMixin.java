package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.LightType;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
/** Forge's bundled Mixin predates interface injectors. Keep vanilla light-engine delegation when inactive. */
@Mixin(IBlockDisplayReader.class)
public interface LightMixin {
 @Shadow WorldLightManager getLightEngine();
 /** @author atemoyacode-sudo @reason Supply supplemental light outside the native vertical range. */
 @Overwrite default int getBrightness(LightType layer,BlockPos pos){
  return BridgeState.outside(pos.getY()) ? BridgeState.light(layer,pos) : getLightEngine().getLayerListener(layer).getLightValue(pos);
 }
 /** @author atemoyacode-sudo @reason Supply supplemental light outside the native vertical range. */
 @Overwrite default int getRawBrightness(BlockPos pos,int skyDark){
  return BridgeState.outside(pos.getY()) ? Math.max(BridgeState.light(LightType.BLOCK,pos),BridgeState.light(LightType.SKY,pos)-skyDark) : getLightEngine().getRawBrightness(pos,skyDark);
 }
}
