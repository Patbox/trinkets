package eu.pb4.trinkets.impl.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Util;

public interface ClientAbstraction {
    ClientAbstraction INSTANCE = Util.make(() -> {
        try {
            return (ClientAbstraction) Class.forName(
                    "eu.pb4.trinkets.impl.platform." +
                            (CommonAbstraction.IS_FABRIC ? "fabric.FabricClientAbstraction" : "neo.NeoClientAbstraction")).getField("INSTANCE").get(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    });

    static ClientAbstraction get() {
        return INSTANCE;
    }

    <T extends CustomPacketPayload> void registerGlobalReceiverPlay(CustomPacketPayload.Type<T> type, PlayPacketReceiver<T> receiver);

    void registerClientReloadListener(Identifier identifier, PreparableReloadListener instance, Identifier... requires);

    void registerClientTagsLoadedEvent(Runnable afterTagsLoaded);

    interface PlayPacketReceiver<T> {
        void receive(Minecraft minecraft, LocalPlayer player, T payload);
    }
}
