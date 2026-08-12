package com.ricedotwho.dtmap.mixin;

import com.ricedotwho.dtmap.events.ChatEvents;
import com.ricedotwho.dtmap.features.SecretSpawnTimer;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
    private void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundPingPacket packet1 && packet1.getId() != 0) {
            SecretSpawnTimer.INSTANCE.tick();
            return;
        }
        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            var component = chatPacket.content();
            var message = component.getString();
            var unformatted = ChatFormatting.stripFormatting(message);
            ChatEvents.INSTANCE.getSYSTEM_MESSAGE_EVENT().invoker().receiveSystemMessage(new ChatEvents.SystemMessageEvent.Data(component, message, unformatted));
        }
    }
}
