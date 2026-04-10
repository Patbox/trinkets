package eu.pb4.trinkets.impl.platform.neo;

import eu.pb4.trinkets.impl.platform.ClientAbstraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public record NeoClientAbstraction() implements ClientAbstraction {
    public static final NeoClientAbstraction INSTANCE = new NeoClientAbstraction();

    @Override
    public <T extends CustomPacketPayload> void registerGlobalReceiverPlay(CustomPacketPayload.Type<T> type, PlayPacketReceiver<T> receiver) {
        NeoCommonAbstraction.INSTANCE.addLateAction(bus -> {
            bus.addListener(RegisterClientPayloadHandlersEvent.class, e -> {
                e.register(type, (p, ctx) -> receiver.receive(Minecraft.getInstance(), (LocalPlayer) ctx.player(), p));
            });
        });
    }
}
