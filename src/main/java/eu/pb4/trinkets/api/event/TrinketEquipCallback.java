package eu.pb4.trinkets.api.event;

import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketEquipCallback {
	Event<Identifier, TrinketEquipCallback> EVENT = YumiEvents.EVENTS.create(TrinketEquipCallback.class,
	listeners -> (stack, slot, entity) -> {
		for (TrinketEquipCallback listener: listeners){
			listener.onEquip(stack, slot, entity);
		}
	});

	/**
	 * Called when an entity equips a trinket, after the {@link eu.pb4.trinkets.api.callback.TrinketCallback#onEquip} method of the Trinket
	 *
	 * @param stack The stack being equipped
	 * @param slot The slot the stack is equipped to
	 * @param entity The entity that equipped the stack
	 */
	void onEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity);
}