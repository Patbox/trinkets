package eu.pb4.trinkets.api.datagen;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ConditionBuilder {
    ConditionBuilder mergeWithAnd();

    ConditionBuilder mergeWithOr();

    ConditionBuilder replace();

    ConditionBuilder andAllOf(Identifier... identifier);

    ConditionBuilder andAnyOf(Identifier... identifier);

    ConditionBuilder orAllOf(Identifier... identifier);

    ConditionBuilder orAnyOf(Identifier... identifier);
}
