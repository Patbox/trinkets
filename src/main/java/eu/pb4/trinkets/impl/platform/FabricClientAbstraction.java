package eu.pb4.trinkets.impl.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FabricClientAbstraction() implements ClientAbstraction {
    @Override
    public <T extends CustomPacketPayload> void registerGlobalReceiverPlay(CustomPacketPayload.Type<T> type, PlayPacketReceiver<T> receiver) {
        ClientPlayNetworking.registerGlobalReceiver(type, (p, ctx) -> receiver.receive(ctx.client(), ctx.player(), p));
    }
}
