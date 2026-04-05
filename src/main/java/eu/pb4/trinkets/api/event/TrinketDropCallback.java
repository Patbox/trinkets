package eu.pb4.trinkets.api.event;

import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketDropRule;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketDropCallback {
	Event<Identifier, TrinketDropCallback> EVENT = YumiEvents.EVENTS.create(TrinketDropCallback.class,
	listeners -> (rule, stack, ref, entity) -> {
		for (TrinketDropCallback listener : listeners) {
			rule = listener.drop(rule, stack, ref, entity);
		}
		return rule;
	});

	TrinketDropRule drop(TrinketDropRule rule, ItemStack stack, TrinketSlotAccess ref, LivingEntity entity);
}