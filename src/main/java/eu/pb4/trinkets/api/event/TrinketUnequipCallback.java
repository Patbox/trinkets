package eu.pb4.trinkets.api.event;

import eu.pb4.trinkets.api.SlotReference;
import eu.pb4.trinkets.api.Trinket;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketUnequipCallback {
	Event<TrinketUnequipCallback> EVENT = EventFactory.createArrayBacked(TrinketUnequipCallback.class,
	listeners -> (stack, slot, entity) -> {
		for (TrinketUnequipCallback listener: listeners){
			listener.onUnequip(stack, slot, entity);
		}
	});

	/**
	 * Called when an entity un-equips a trinket, after the {@link Trinket#onUnequip} method of the Trinket
	 *
	 * @param stack The stack being unequipped
	 * @param slot The slot the stack was unequipped from
	 * @param entity The entity that unequipped the stack
	 */
	void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity);
}