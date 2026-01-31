package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.SlotReference;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSaveData;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.Trinket;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.impl.TrinketsNetwork;
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
				TrinketsApi.getTrinketComponent((LivingEntity) entity).ifPresent(trinkets -> {
					for (Map.Entry<String, TrinketSaveData.Metadata> entry : payload.inventoryUpdates().entrySet()) {
						String[] split = entry.getKey().split("/");
						String group = split[0];
						String slot = split[1];
						Map<String, TrinketInventory> slots = trinkets.getInventory().get(group);
						if (slots != null) {
							TrinketInventory inv = slots.get(slot);
							if (inv != null) {
								inv.applySyncMetadata(entry.getValue());
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
						Map<String, TrinketInventory> slots = trinkets.getInventory().get(group);
						if (slots != null) {
							TrinketInventory inv = slots.get(slot);
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
				TrinketsApi.getTrinketComponent(entity).ifPresent(comp -> {
					Map<String, TrinketInventory> groupMap = comp.getInventory().get(payload.group());
					if (groupMap != null) {
						TrinketInventory inv = groupMap.get(payload.slot());
						if (payload.index() < inv.getContainerSize()) {
							ItemStack stack = inv.getItem(payload.index());
							SlotReference ref = new SlotReference(inv, payload.index());
							Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
							trinket.onBreak(stack, ref, entity);
						}
					}
				});
			}

		});
	}
}