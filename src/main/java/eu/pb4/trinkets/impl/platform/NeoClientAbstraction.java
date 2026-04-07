package eu.pb4.trinkets.impl.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record NeoClientAbstraction() implements ClientAbstraction {
    public static NeoClientAbstraction INSTANCE = new NeoClientAbstraction();

    @Override
    public <T extends CustomPacketPayload> void registerGlobalReceiverPlay(CustomPacketPayload.Type<T> type, PlayPacketReceiver<T> receiver) {
        NeoServerAbstraction.INSTANCE.addLateAction(bus -> {
            bus.addListener(RegisterClientPayloadHandlersEvent.class, e -> {
                e.register(type, (p, ctx) -> receiver.receive(Minecraft.getInstance(), (LocalPlayer) ctx.player(), p));
            });
        });
    }
}
