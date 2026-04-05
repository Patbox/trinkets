package eu.pb4.trinkets.api.event;

import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketUnequipCallback {
	Event<Identifier, TrinketUnequipCallback> EVENT = YumiEvents.EVENTS.create(TrinketUnequipCallback.class,
	listeners -> (stack, slot, entity) -> {
		for (TrinketUnequipCallback listener: listeners){
			listener.onUnequip(stack, slot, entity);
		}
	});

	/**
	 * Called when an entity un-equips a trinket, after the {@link eu.pb4.trinkets.api.callback.TrinketCallback#onUnequip} method of the Trinket
	 *
	 * @param stack The stack being unequipped
	 * @param slot The slot the stack was unequipped from
	 * @param entity The entity that unequipped the stack
	 */
	void onUnequip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity);
}