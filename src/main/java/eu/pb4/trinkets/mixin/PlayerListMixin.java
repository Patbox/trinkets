package eu.pb4.trinkets.mixin;


import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSaveData;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Syncs slot data to player's client on login
 *
 * @author C4
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;initInventoryMenu()V"), method = "placeNewPlayer")
	private void onPlayerConnect(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
		EntitySlotLoader.SERVER.sync(player);
		this.syncSlots(player);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;initInventoryMenu()V"), method = "respawn")
	private void onPlayerRespawn(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer newServerPlayer) {
		this.syncSlots(player);
	}

	@Unique
	private void syncSlots(ServerPlayer player) {
		((TrinketPlayerScreenHandler) player.inventoryMenu).trinkets$updateTrinketSlots(false);
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
			Map<String, TrinketSaveData.Metadata> tag = new HashMap<>();
			Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

			for (TrinketInventory trinketInventory : inventoriesToSend) {
				tag.put(trinketInventory.getSlotType().getId(), trinketInventory.getSyncMetadata());
			}
			ServerPlayNetworking.send(player, new SyncInventoryPayload(player.getId(), Map.of(), tag));
			inventoriesToSend.clear();
		});
	}
}