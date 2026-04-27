package eu.pb4.trinkets.api;

import eu.pb4.trinkets.impl.TrinketsMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Trinkets slot ids provided by trinkets by default.
 */
public final class BuiltInTrinketConditions {
    public static final Identifier NONE = of("none");
    public static final Identifier ALL = of("all");
    public static final Identifier DEFAULT = of("default");
    public static final Identifier TAG = of("tag");
    public static final Identifier COMPONENT = of("component");
    public static final Identifier ATTRIBUTES = of("attributes");


    private BuiltInTrinketConditions() {}

    private static Identifier of(String slot) {
        return Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, slot);
    }
}
