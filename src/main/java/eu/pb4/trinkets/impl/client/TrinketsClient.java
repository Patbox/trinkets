package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.impl.*;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_INVENTORY, (payload, context) -> {
			Minecraft client = context.client();
			Entity entity = client.level.getEntity(payload.entityId());
			if (entity instanceof LivingEntity) {
				TrinketsApi.getTrinketAttachment((LivingEntity) entity).map(x -> (LivingEntityTrinketComponent) x).ifPresent(trinkets -> {
					System.out.println("Update");

					for (var entry : payload.inventorySize().entrySet()) {
						System.out.println(entry.getKey() + " = " + entry.getValue());
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
						String[] split = entry.getKey().split("/");
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
				});
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_SLOTS, (payload, context) -> {

			EntitySlotLoader.CLIENT.setSlots(payload.map());
			LocalPlayer player = context.player();

			if (player != null) {
				((TrinketPlayerScreenHandler) player.inventoryMenu).trinkets$updateTrinketSlots(true);

				Minecraft client = context.client();
				if (client.screen instanceof TrinketScreen trinketScreen) {
					trinketScreen.trinkets$updateTrinketSlots();
				}

				for (Player clientWorldPlayer : context.player().level().players()) {
					((TrinketPlayerScreenHandler) clientWorldPlayer.inventoryMenu).trinkets$updateTrinketSlots(true);
				}
			}

		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.BREAK, (payload, context) -> {

			Minecraft client = context.client();
			Entity e = client.level.getEntity(payload.entityId());
			if (e instanceof LivingEntity entity) {
				TrinketsApi.getTrinketAttachment(entity).ifPresent(comp -> {
					var groupMap = comp.getInventory().get(payload.group());
					if (groupMap != null) {
						var inv = groupMap.get(payload.slot());
						if (payload.index() < inv.getContainerSize()) {
							ItemStack stack = inv.getItem(payload.index());
							TrinketSlotAccess ref = new TrinketSlotAccess(inv, payload.index());
							var trinket = TrinketCallback.getCallback(stack);
							trinket.onBreak(stack, ref, entity);
						}
					}
				});
			}

		});
	}
}