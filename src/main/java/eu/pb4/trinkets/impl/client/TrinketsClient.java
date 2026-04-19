package eu.pb4.trinkets.impl.client;

import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.client.ClientModInitializer;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.TrinketsNetwork;
import eu.pb4.trinkets.impl.client.render.ClientTrinketsManager;
import eu.pb4.trinkets.impl.client.render.types.TrinketRenderElements;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.platform.ClientAbstraction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TrinketsClient implements ClientModInitializer {
    public static SlotGroup activeGroup;
    public static SlotType activeType;
    public static SlotGroup quickMoveGroup;
    public static SlotType quickMoveType;
    public static int quickMoveTimer;

    @Override
    public void onInitializeClient(ModContainer modContainer) {
        TrinketRenderElements.bootstrap();

        ClientAbstraction.get().registerGlobalReceiverPlay(TrinketsNetwork.SYNC_INVENTORY, (client, player, payload) -> {
            Entity entity = client.level.getEntity(payload.entityId());
            if (entity instanceof LivingEntity livingEntity) {
                var trinkets = LivingEntityTrinketAttachment.get(livingEntity);

                for (var entry : payload.inventorySize().entrySet()) {
                    var inv = trinkets.getInventory(entry.getKey());
                    if (inv != null) {
                        inv.setSlotCount(entry.getValue());
                    }
                }

                if (entity instanceof Player && ((Player) entity).inventoryMenu instanceof TrinketInventoryMenu screenHandler) {
                    screenHandler.trinkets$updateTrinketSlots(false);
                    TrinketScreenManager.tryUpdateTrinketsSlot();
                }

                for (var entry : payload.contentUpdates().entrySet()) {
                    var access = trinkets.getSlotAccess(entry.getKey());
                    if (access != null) {
                        access.set(entry.getValue());
                    }
                }
            }
        });
        ClientAbstraction.get().registerGlobalReceiverPlay(TrinketsNetwork.SYNC_SLOTS, (client, player, payload) -> {
            EntitySlotLoader.CLIENT.setSlots(payload.map());

            if (player != null) {
                ((TrinketInventoryMenu) player.inventoryMenu).trinkets$updateTrinketSlots(true);

                if (client.screen instanceof TrinketScreen trinketScreen) {
                    trinketScreen.trinkets$updateTrinketSlots();
                }

                for (Player clientWorldPlayer : player.level().players()) {
                    ((TrinketInventoryMenu) clientWorldPlayer.inventoryMenu).trinkets$updateTrinketSlots(true);
                }
            }

        });
        ClientAbstraction.get().registerGlobalReceiverPlay(TrinketsNetwork.BREAK, (client, player, payload) -> {
            Entity e = client.level.getEntity(payload.entityId());
            if (e instanceof LivingEntity livingEntity) {
                var comp = LivingEntityTrinketAttachment.get(livingEntity);
                var ref = comp.getSlotAccess(payload.reference());
                if (ref != null) {
                    var stack = ref.get();
                    var trinket = TrinketCallback.getCallback(stack);
                    trinket.onBreak(stack, ref, livingEntity);
                }
            }
        });

        ClientAbstraction.INSTANCE.registerClientReloadListener(Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "client_trinkets"), ClientTrinketsManager.INSTANCE, List.of(),
                List.of(ClientAbstraction.INSTANCE.getClientModelResourceReloaderId()));
        ClientAbstraction.INSTANCE.registerClientTagsLoadedEvent(ClientTrinketsManager.INSTANCE::updateItemMap);
    }
}