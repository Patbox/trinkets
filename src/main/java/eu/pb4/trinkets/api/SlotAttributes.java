package eu.pb4.trinkets.api;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class SlotAttributes {
	private static final Map<String, Identifier> CACHED_IDS = Maps.newHashMap();
	private static final Map<String, Holder<Attribute>> CACHED_ATTRIBUTES = Maps.newHashMap();

	/**
	 * Adds an Entity Attribute Modifier for slot count to the provided multimap
	 */
	public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map, String slot, Identifier identifier, double amount,
			AttributeModifier.Operation operation) {
		CACHED_ATTRIBUTES.putIfAbsent(slot, Holder.direct(new SlotEntityAttribute(slot)));
		map.put(CACHED_ATTRIBUTES.get(slot), new AttributeModifier(identifier, amount, operation));
	}

	public static Identifier getIdentifier(SlotReference ref) {
		String key = ref.getId();
		return CACHED_IDS.computeIfAbsent(key, Identifier::parse);
	}

	public static class SlotEntityAttribute extends Attribute {
		public String slot;

		private SlotEntityAttribute(String slot) {
			super("trinkets.slot." + slot.replace("/", "."), 0);
			this.slot = slot;
		}
	}
}