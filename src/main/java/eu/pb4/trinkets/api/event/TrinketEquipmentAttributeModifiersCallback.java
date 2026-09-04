package eu.pb4.trinkets.api.event;

import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

public interface TrinketEquipmentAttributeModifiersCallback {
	Event<Identifier, TrinketEquipmentAttributeModifiersCallback> EVENT = YumiEvents.EVENTS.create(TrinketEquipmentAttributeModifiersCallback.class,
	listeners -> (stack, slot, entity, slotId, consumer) -> {
		for (var listener : listeners) {
			listener.forEachTrinketModifier(stack, slot, entity, slotId, consumer);
		}
	});

	/**
	 * Called when entity attributes are called with addition of {@link eu.pb4.trinkets.api.callback.TrinketCallback#forEachTrinketModifier(ItemStack, TrinketSlotAccess, LivingEntity, Identifier, BiConsumer)}
	 * <p>
	 * If modifiers do not change based on stack, slot, or entity, caching based on passed identifier
	 * should be considered
	 *
	 * @param stack ItemStack being polled for modifiers
	 * @param slot the {@link TrinketSlotAccess} for the {@link eu.pb4.trinkets.api.TrinketInventory} the Trinket is relevant to
	 * @param entity the LivingEntity holding the Trinket
	 * @param slotIdentifier The Identifier to use for creating attributes
	 * @param consumer the consumer of attributes
	 */
	void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier slotIdentifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer);
}