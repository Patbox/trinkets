package eu.pb4.trinkets.mixin;

import eu.pb4.trinkets.api.TrinketSlotReference;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void sendTrinketData(ServerPlayer player, Consumer<Packet<?>> broadcast, CallbackInfo ci) {
        if (!(this.entity instanceof LivingEntity livingEntity)) {
            return;
        }

        var trinket = LivingEntityTrinketAttachment.get(livingEntity);

        var slotCount = new HashMap<String, Integer>();
        var items = new HashMap<TrinketSlotReference, ItemStack>();

        for (var y : trinket.inventory.values()) {
            var id = y.slotType().getId();
            if (y.getContainerSize() != y.slotType().amount()) {
                slotCount.put(id, y.getContainerSize());
            }

            for (int i = 0; i < y.getContainerSize(); i++) {
                var item = y.getItem(i);
                if (!item.isEmpty()) {
                    items.put(new TrinketSlotReference(id, i), item.copy());
                }
            }
        }

        if (!slotCount.isEmpty() || !items.isEmpty()) {
            broadcast.accept(new ClientboundCustomPayloadPacket(new SyncInventoryPayload(this.entity.getId(), items, slotCount)));
        }
    }
}
