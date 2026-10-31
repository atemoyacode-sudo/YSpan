package jp.arils.belowzero.mixin;
import jp.arils.belowzero.BridgeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class NetworkMixin {
 private ClientPacketListener bz$self(){return (ClientPacketListener)(Object)this;}
 @Inject(method="handleLogin",at=@At("HEAD"))
 private void bz$connect(CallbackInfo ci){if(Minecraft.getInstance().isSameThread())BridgeState.connected(bz$self());}
 @Inject(method="handleRespawn",at=@At("HEAD"))
 private void bz$world(CallbackInfo ci){if(Minecraft.getInstance().isSameThread())BridgeState.worldChanged(bz$self());}
 @Inject(method={"handleLogin","handleRespawn"},at=@At("RETURN"))
 private void bz$hello(CallbackInfo ci){BridgeState.handshake(bz$self());}
 @Inject(method="handleCustomPayload",at=@At("HEAD"),cancellable=true)
 private void bz$payload(ClientboundCustomPayloadPacket packet,CallbackInfo ci){
  // Vanilla PacketUtils performs the first Netty-to-main-thread handoff. Do not process twice.
  if(!Minecraft.getInstance().isSameThread())return;
  ResourceLocation id=packet.getIdentifier();
  boolean register=id.toString().equals("minecraft:register"),unregister=id.toString().equals("minecraft:unregister");
  boolean own=id.equals(BridgeState.CHANNEL);
  if(!own&&!register&&!unregister)return;
  FriendlyByteBuf buf=packet.getData();byte[] data=new byte[buf.readableBytes()];
  try{buf.readBytes(data);}finally{buf.release();}
  if(own){BridgeState.receive(bz$self(),data);ci.cancel();}
  else BridgeState.serverChannels(bz$self(),data,register);
  // Leave REGISTER/UNREGISTER visible to vanilla and other client mods.
 }
}
