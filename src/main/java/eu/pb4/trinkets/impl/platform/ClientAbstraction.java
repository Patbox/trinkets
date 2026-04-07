package eu.pb4.trinkets.impl.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ClientAbstraction {
    static ClientAbstraction get() {
        return CommonAbstraction.IS_FABRIC ? new FabricClientAbstraction() : NeoClientAbstraction.INSTANCE;
    }

    <T extends CustomPacketPayload> void registerGlobalReceiverPlay(CustomPacketPayload.Type<T> type, PlayPacketReceiver<T> receiver);

    interface PlayPacketReceiver<T> {
        void receive(Minecraft minecraft, LocalPlayer player, T payload);
    }
}
