package eu.pb4.trinkets.api.event;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketEquipmentChangedCallback {
	Event<TrinketEquipmentChangedCallback> EVENT = EventFactory.createArrayBacked(TrinketEquipmentChangedCallback.class,
	listeners -> (previous, stack, slot, entity) -> {
		for (var listener: listeners){
			listener.onEquipmentChanged(previous, stack, slot, entity);
		}
	});

	/**
	 * Called when an entity equips a trinket, after the {@link Trinket#onEquip} method of the Trinket
	 *
	 * @param previous Previously equipped item stack
	 * @param stack The new stack
	 * @param slot The slot the stack is equipped to
	 * @param entity The entity that equipped the stack
	 */
	void onEquipmentChanged(ItemStack previous, ItemStack stack, TrinketSlotAccess slot, LivingEntity entity);
}