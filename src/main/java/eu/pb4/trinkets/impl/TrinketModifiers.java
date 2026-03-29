package eu.pb4.trinkets.impl;

import com.google.common.collect.Multimap;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.api.component.TrinketsAttributeModifiersComponent;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

@Deprecated(forRemoval = true)
public class TrinketModifiers {

	//internalizes getTrinket and slotIdentifier, both which typically are generated just before the modifiers call anyway
	public static Multimap<Holder<Attribute>, AttributeModifier> get(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		Multimap<Holder<Attribute>, AttributeModifier> map = TrinketCallback.getCallback(stack).getModifiers(stack, slot, entity, SlotAttributes.getIdentifier(slot));
		if (stack.has(TrinketDataComponents.ATTRIBUTE_MODIFIERS)) {
			for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketDataComponents.ATTRIBUTE_MODIFIERS, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
				map.put(entry.attribute(), entry.modifier());
			}
		}
		return map;
	}

	//overload if a custom method for retrieving the trinket is used. Also exposes the slotIdentifier if custom on that is needed
	public static Multimap<Holder<Attribute>, AttributeModifier> get(TrinketCallback trinket, ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier slotIdentifier){
		Multimap<Holder<Attribute>, AttributeModifier> map = trinket.getModifiers(stack, slot, entity, slotIdentifier);
		if (stack.has(TrinketDataComponents.ATTRIBUTE_MODIFIERS)) {
			for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketDataComponents.ATTRIBUTE_MODIFIERS, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
				if (entry.slot().isEmpty() || entry.slot().get().equals(slot.inventory().slotType().getId())) {
					map.put(entry.attribute(), entry.modifier());
				}
			}
		}
		return map;
	}
}