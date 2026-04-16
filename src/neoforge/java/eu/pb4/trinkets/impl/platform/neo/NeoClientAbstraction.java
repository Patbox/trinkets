package eu.pb4.trinkets.impl.platform.neo;

import eu.pb4.trinkets.impl.platform.ClientAbstraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

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

    @Override
    public void registerClientReloadListener(Identifier identifier, PreparableReloadListener instance, Identifier... requires) {
        NeoForge.EVENT_BUS.addListener(AddClientReloadListenersEvent.class, e -> {
            e.addListener(identifier, instance);

            for (var r : requires) {
                e.addDependency(r, identifier);
            }
        });
    }

    @Override
    public void registerClientTagsLoadedEvent(Runnable afterTagsLoaded) {
        NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, e -> {
            if (e.shouldUpdateStaticData()) {
                afterTagsLoaded.run();
            }
        });
    }
}
