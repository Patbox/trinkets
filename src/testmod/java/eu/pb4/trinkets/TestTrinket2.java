package eu.pb4.trinkets;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public class TestTrinket2 extends Item implements TrinketCallback {

	public TestTrinket2(Properties settings) {
		super(settings);
	}

	@Override
	public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier id,
									   BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {		// un-comment to check - testing the composition of the new attribute suffix
		// TrinketsTest.LOGGER.info(TrinketModifiers.toSlotReferencedModifier(new EntityAttributeModifier(id.withSuffixedPath("trinkets-testmod/movement_speed"),
		//		0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot));
		AttributeModifier speedModifier = new AttributeModifier(id.withSuffix("trinkets-testmod/movement_speed"),
				0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		consumer.accept(Attributes.MOVEMENT_SPEED, speedModifier);
	}
}