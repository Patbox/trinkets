package eu.pb4.trinkets.mixin;


import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsConfig;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.payload.SyncConfigPayload;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Syncs slot data to player's client on login
 *
 * @author C4
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow
    public abstract List<ServerPlayer> getPlayers();

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;sendLevelInfo(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;)V"), method = "placeNewPlayer")
    private void onPlayerConnect(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        connection.send(new ClientboundCustomPayloadPacket(new SyncConfigPayload(TrinketsConfig.instance.gameplay)));
        EntitySlotLoader.SERVER.sync(player);
        TrinketsMain.syncSlots(player, false);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addRespawnedPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"), method = "respawn")
    private void onPlayerRespawn(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer newServerPlayer) {
        TrinketsMain.syncSlots(newServerPlayer, false);
    }

    @Inject(at = @At("TAIL"), method = "reloadResources")
    private void onReloadResource(CallbackInfo ci) {
        EntitySlotLoader.SERVER.sync(this.getPlayers());
    }
}