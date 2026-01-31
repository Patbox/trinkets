package eu.pb4.trinkets;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import eu.pb4.trinkets.api.SlotReference;
import eu.pb4.trinkets.api.TrinketItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

public class TestTrinket2 extends TrinketItem {

	public TestTrinket2(Properties settings) {
		super(settings);
	}

	@Override
	public Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, Identifier id) {
		// un-comment to check - testing the composition of the new attribute suffix
		// TrinketsTest.LOGGER.info(TrinketModifiers.toSlotReferencedModifier(new EntityAttributeModifier(id.withSuffixedPath("trinkets-testmod/movement_speed"),
		//		0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot));
		Multimap<Holder<Attribute>, AttributeModifier> modifiers = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
		AttributeModifier speedModifier = new AttributeModifier(id.withSuffix("trinkets-testmod/movement_speed"),
				0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifiers.put(Attributes.MOVEMENT_SPEED, speedModifier);
		return modifiers;
	}
}