package eu.pb4.trinkets.impl.platform.neo;

import eu.pb4.trinkets.impl.platform.ClientAbstraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.List;

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
    public void registerClientReloadListener(Identifier identifier, PreparableReloadListener instance, List<Identifier> requires, List<Identifier> requiredBy) {
        NeoCommonAbstraction.INSTANCE.addLateAction(bus -> bus.addListener(AddClientReloadListenersEvent.class, e -> {
            e.addListener(identifier, instance);

            for (var r : requires) {
                e.addDependency(r, identifier);
            }

            for (var r : requiredBy) {
                e.addDependency(identifier, r);
            }
        }));
    }

    @Override
    public void registerClientTagsLoadedEvent(Runnable afterTagsLoaded) {
        NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, e -> {
            if (e.shouldUpdateStaticData()) {
                afterTagsLoaded.run();
            }
        });
    }

    @Override
    public Identifier getClientModelResourceReloaderId() {
        return VanillaClientListeners.MODELS;
    }
}
