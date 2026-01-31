package eu.pb4.trinkets.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.api.TrinketInventory;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Copies slot EAMs to players client-side when changing dimensions
 *
 * @author C4
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setId(I)V"), method = "handleRespawn")
    private void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo info, @Local(ordinal = 0) LocalPlayer clientPlayerEntity, @Local(ordinal = 1) LocalPlayer clientPlayerEntity2)  {
        if (packet.shouldKeep(ClientboundRespawnPacket.KEEP_ATTRIBUTE_MODIFIERS)) {
            TrinketInventory.copyFrom(clientPlayerEntity, clientPlayerEntity2);
            ((TrinketPlayerScreenHandler) clientPlayerEntity2.inventoryMenu).trinkets$updateTrinketSlots(false);
        }
    }
}
