package eu.pb4.trinkets.impl.platform.fabric;

import eu.pb4.trinkets.impl.platform.ClientAbstraction;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FabricClientAbstraction() implements ClientAbstraction {
    public static final ClientAbstraction INSTANCE = new FabricClientAbstraction();

    @Override
    public <T extends CustomPacketPayload> void registerGlobalReceiverPlay(CustomPacketPayload.Type<T> type, PlayPacketReceiver<T> receiver) {
        ClientPlayNetworking.registerGlobalReceiver(type, (p, ctx) -> receiver.receive(ctx.client(), ctx.player(), p));
    }
}
