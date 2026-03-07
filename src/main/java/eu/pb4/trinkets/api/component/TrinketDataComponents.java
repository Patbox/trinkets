package eu.pb4.trinkets.api.component;

import net.minecraft.core.component.DataComponentType;

public class TrinketDataComponents {
    public static final DataComponentType<TrinketEquippable> EQUIPMENT = DataComponentType.<TrinketEquippable>builder()
            .persistent(TrinketEquippable.CODEC).networkSynchronized(TrinketEquippable.STREAM_CODEC).build();

    public static final DataComponentType<TrinketsAttributeModifiersComponent> ATTRIBUTE_MODIFIERS = DataComponentType.<TrinketsAttributeModifiersComponent>builder()
            .persistent(TrinketsAttributeModifiersComponent.CODEC).networkSynchronized(TrinketsAttributeModifiersComponent.PACKET_CODEC).build();
}
