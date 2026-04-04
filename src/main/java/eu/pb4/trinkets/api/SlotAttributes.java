package eu.pb4.trinkets.api;

import com.google.common.collect.Maps;
import eu.pb4.trinkets.impl.TrinketsMain;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Map;

public final class SlotAttributes {
    private static final Map<String, Identifier> CACHED_IDS = Maps.newHashMap();
    private static final Map<String, Holder<Attribute>> CACHED_ATTRIBUTES = Maps.newHashMap();

    /**
     * Provides an attribute that controls specific slots size.
     * When it's called for first time, it will register it and return the value.
     * Any subsequent call will return the same value for specified slot.
     *
     * This methods should only be called on mod / registry initialization, as once the registries are frozen it will crash the game!
     *
     * @param slot the id of the slot
     * @return A attribute holder that controls specific slot.
     */
    public static Holder<Attribute> createAttributeForSlot(String slot) {
        synchronized (CACHED_ATTRIBUTES) {
            var val = CACHED_ATTRIBUTES.get(slot);
            if (val != null) {
                return val;
            }

            val = Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "slot_count/" + slot), new SlotModifyingAttribute(slot));
            CACHED_ATTRIBUTES.put(slot, val);
            return val;
        }
    }


    public static Identifier getIdentifier(TrinketSlotAccess ref) {
        String key = ref.getSerializedName();
        return CACHED_IDS.computeIfAbsent(key, Identifier::parse);
    }

    public static class SlotModifyingAttribute extends Attribute {
        public String slot;

        private SlotModifyingAttribute(String slot) {
            super("trinkets.slot." + slot.replace("/", "."), 0);
            this.slot = slot;
        }
    }
}