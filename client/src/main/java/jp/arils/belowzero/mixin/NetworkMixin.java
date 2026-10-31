package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.client.Minecraft;import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class NetworkMixin {
 @Inject(method={"handleLogin","handleRespawn"},at=@At("HEAD")) private void bz$reset(CallbackInfo ci){if(Minecraft.getInstance().isSameThread())BridgeState.reset();}
 @Inject(method={"handleLogin","handleRespawn"},at=@At("RETURN")) private void bz$hello(CallbackInfo ci){BridgeState.handshake();}
 @Inject(method="handleCustomPayload",at=@At("HEAD"),cancellable=true) private void bz$payload(ClientboundCustomPayloadPacket p,CallbackInfo ci){
  if(!p.getIdentifier().equals(BridgeState.CHANNEL))return;
  FriendlyByteBuf buf=p.getData();byte[] data=new byte[buf.readableBytes()];try{buf.readBytes(data);}finally{buf.release();}
  Minecraft.getInstance().execute(()->BridgeState.receive(data));ci.cancel();
 }
}
