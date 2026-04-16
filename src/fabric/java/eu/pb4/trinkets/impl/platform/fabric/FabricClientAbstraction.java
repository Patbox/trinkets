package eu.pb4.trinkets.impl.platform.fabric;

import eu.pb4.trinkets.impl.platform.ClientAbstraction;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public record FabricClientAbstraction() implements ClientAbstraction {
    public static final ClientAbstraction INSTANCE = new FabricClientAbstraction();

    @Override
    public <T extends CustomPacketPayload> void registerGlobalReceiverPlay(CustomPacketPayload.Type<T> type, PlayPacketReceiver<T> receiver) {
        ClientPlayNetworking.registerGlobalReceiver(type, (p, ctx) -> receiver.receive(ctx.client(), ctx.player(), p));
    }

    @Override
    public void registerClientReloadListener(Identifier identifier, PreparableReloadListener instance, Identifier... requires) {
        var loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(identifier, instance);

        for (var r : requires) {
            loader.addListenerOrdering(r, identifier);
        }
    }

    @Override
    public void registerClientTagsLoadedEvent(Runnable afterTagsLoaded) {
        CommonLifecycleEvents.TAGS_LOADED.register(((registryAccess, b) -> {
            if (b) {
                afterTagsLoaded.run();
            }
        }));
    }
}
