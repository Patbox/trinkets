package eu.pb4.trinkets.api.event;

import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketEquipmentChangedCallback {
	Event<Identifier, TrinketEquipmentChangedCallback> EVENT = YumiEvents.EVENTS.create(TrinketEquipmentChangedCallback.class,
	listeners -> (previous, stack, slot, entity) -> {
		for (var listener: listeners){
			listener.onEquipmentChanged(previous, stack, slot, entity);
		}
	});

	/**
	 * Called when an entity equips a trinket, after the {@link eu.pb4.trinkets.api.callback.TrinketCallback#onEquip} method of the Trinket
	 *
	 * @param previous Previously equipped item stack
	 * @param stack The new stack
	 * @param slot The slot the stack is equipped to
	 * @param entity The entity that equipped the stack
	 */
	void onEquipmentChanged(ItemStack previous, ItemStack stack, TrinketSlotAccess slot, LivingEntity entity);
}