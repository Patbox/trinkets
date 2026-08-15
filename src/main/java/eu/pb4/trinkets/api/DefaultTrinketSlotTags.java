package eu.pb4.trinkets.api;

import eu.pb4.trinkets.impl.TrinketsMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Trinkets slot ids provided by trinkets by default.
 */
public final class DefaultTrinketSlotTags {
    // Head slots
    public static final TagKey<Item> HEAD_FACE = of("head/face");
    public static final TagKey<Item> HEAD_HAT = of("head/hat");
    // Chest slots
    public static final TagKey<Item> CHEST_BACK = of("chest/back");
    public static final TagKey<Item> CHEST_CAPE = of("chest/cape");
    public static final TagKey<Item> CHEST_NECKLACE = of("chest/necklace");
    // Mainhand slots
    public static final TagKey<Item> HAND_GLOVE = of("hand/glove");
    public static final TagKey<Item> HAND_RING = of("hand/ring");
    // Offhand slots
    public static final TagKey<Item> OFFHAND_GLOVE = of("offhand/glove");
    public static final TagKey<Item> OFFHAND_RING = of("offhand/ring");
    // Legs
    public static final TagKey<Item> LEGS_BELT = of("legs/belt");
    // Feet
    public static final TagKey<Item> FEET_SHOES = of("feet/shoes");
    public static final TagKey<Item> FEET_AGLET = of("feet/aglet");

    // Allows all slots
    public static final TagKey<Item> ALL = of("all");
    // Generic hand slots
    public static final TagKey<Item> ANY_GLOVE = of("any_glove");
    public static final TagKey<Item> ANY_RING = of("any_ring");

    // Should contain all trinkets
    public static final TagKey<Item> TRINKETS = of("trinkets");

    private DefaultTrinketSlotTags() {}

    private static TagKey<Item> of(String slot) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, slot));
    }
}
