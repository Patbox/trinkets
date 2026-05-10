package eu.pb4.trinkets.api.datagen;

import eu.pb4.trinkets.api.TrinketDropRule;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

@ApiStatus.NonExtendable
public interface TrinketSlotTypeBuilder {
    TrinketSlotTypeBuilder replace(boolean replace);

    TrinketSlotTypeBuilder order(int order);

    TrinketSlotTypeBuilder amount(int defaultAmount);
    TrinketSlotTypeBuilder maxStackSize(int maxStackSize);

    TrinketSlotTypeBuilder icon(Identifier icon);

    TrinketSlotTypeBuilder dropRule(TrinketDropRule dropRule);

    TrinketSlotTypeBuilder isVanityOnly(boolean value);

    TrinketSlotTypeBuilder isHidden(boolean value);

    TrinketSlotTypeBuilder quickMoveCondition(Consumer<ConditionBuilder> consumer);

    TrinketSlotTypeBuilder validatorCondition(Consumer<ConditionBuilder> consumer);

    TrinketSlotTypeBuilder tooltipCondition(Consumer<ConditionBuilder> consumer);
    TrinketSlotTypeBuilder interactEquipableCondition(Consumer<ConditionBuilder> consumer);
}
