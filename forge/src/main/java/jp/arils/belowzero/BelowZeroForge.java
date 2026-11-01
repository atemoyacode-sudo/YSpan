package jp.arils.belowzero;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
@Mod("belowzero")
public final class BelowZeroForge {
 public BelowZeroForge(){
  ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
   () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remoteVersion, serverSide) -> true));
  System.out.println("[YSpan] Forge preview loaded; inactive until companion negotiation");
 }
}
