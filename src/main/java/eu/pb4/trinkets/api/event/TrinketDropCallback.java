package eu.pb4.trinkets.api.event;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketDropRule;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketDropCallback {
	Event<TrinketDropCallback> EVENT = EventFactory.createArrayBacked(TrinketDropCallback.class,
	listeners -> (rule, stack, ref, entity) -> {
		for (TrinketDropCallback listener : listeners) {
			rule = listener.drop(rule, stack, ref, entity);
		}
		return rule;
	});

	TrinketDropRule drop(TrinketDropRule rule, ItemStack stack, TrinketSlotAccess ref, LivingEntity entity);
}