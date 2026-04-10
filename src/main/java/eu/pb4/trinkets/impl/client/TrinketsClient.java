package eu.pb4.trinkets.impl.client;

import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.client.ClientModInitializer;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketInventoryImpl;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.impl.TrinketsNetwork;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.platform.ClientAbstraction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

@Environment(EnvType.CLIENT)
public class TrinketsClient implements ClientModInitializer {
    public static SlotGroup activeGroup;
    public static SlotType activeType;
    public static SlotGroup quickMoveGroup;
    public static SlotType quickMoveType;
    public static int quickMoveTimer;

    @Override
    public void onInitializeClient(ModContainer modContainer) {
        ClientAbstraction.get().registerGlobalReceiverPlay(TrinketsNetwork.SYNC_INVENTORY, (client, player, payload) -> {
            Entity entity = client.level.getEntity(payload.entityId());
            if (entity instanceof LivingEntity livingEntity) {
                var trinkets = LivingEntityTrinketAttachment.get(livingEntity);

                for (var entry : payload.inventorySize().entrySet()) {
                    String[] split = entry.getKey().split("/", 2);
                    String group = split[0];
                    String slot = split[1];
                    Map<String, TrinketInventoryImpl> slots = trinkets.getInventoryImpl().get(group);
                    if (slots != null) {
                        TrinketInventoryImpl inv = slots.get(slot);
                        if (inv != null) {
                            inv.setSlotCount(entry.getValue());
                        }
                    }
                }

                if (entity instanceof Player && ((Player) entity).inventoryMenu instanceof TrinketPlayerScreenHandler screenHandler) {
                    screenHandler.trinkets$updateTrinketSlots(false);
                    TrinketScreenManager.tryUpdateTrinketsSlot();
                }

                for (Map.Entry<String, ItemStack> entry : payload.contentUpdates().entrySet()) {
                    String[] split = entry.getKey().split("/", 2);
                    String group = split[0];
                    String slot = split[1];
                    int index = Integer.parseInt(split[2]);
                    Map<String, TrinketInventoryImpl> slots = trinkets.getInventoryImpl().get(group);
                    if (slots != null) {
                        TrinketInventoryImpl inv = slots.get(slot);
                        if (inv != null && index < inv.getContainerSize()) {
                            inv.setItem(index, entry.getValue());
                        }
                    }
                }
            }
        });
        ClientAbstraction.get().registerGlobalReceiverPlay(TrinketsNetwork.SYNC_SLOTS, (client, player, payload) -> {
            EntitySlotLoader.CLIENT.setSlots(payload.map());

            if (player != null) {
                ((TrinketPlayerScreenHandler) player.inventoryMenu).trinkets$updateTrinketSlots(true);

                if (client.screen instanceof TrinketScreen trinketScreen) {
                    trinketScreen.trinkets$updateTrinketSlots();
                }

                for (Player clientWorldPlayer : player.level().players()) {
                    ((TrinketPlayerScreenHandler) clientWorldPlayer.inventoryMenu).trinkets$updateTrinketSlots(true);
                }
            }

        });
        ClientAbstraction.get().registerGlobalReceiverPlay(TrinketsNetwork.BREAK, (client, player, payload) -> {
            Entity e = client.level.getEntity(payload.entityId());
            if (e instanceof LivingEntity livingEntity) {
                var comp = LivingEntityTrinketAttachment.get(livingEntity);
                var groupMap = comp.getInventory().get(payload.group());
                if (groupMap != null) {
                    var inv = groupMap.get(payload.slot());
                    if (payload.index() < inv.getContainerSize()) {
                        ItemStack stack = inv.getItem(payload.index());
                        TrinketSlotAccess ref = new TrinketSlotAccess(inv, payload.index());
                        var trinket = TrinketCallback.getCallback(stack);
                        trinket.onBreak(stack, ref, livingEntity);
                    }
                }
            }
        });
    }
}