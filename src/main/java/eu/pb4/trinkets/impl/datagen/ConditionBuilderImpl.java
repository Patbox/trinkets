package eu.pb4.trinkets.impl.datagen;

import eu.pb4.trinkets.api.datagen.ConditionBuilder;
import eu.pb4.trinkets.impl.slots.SlotTypeImpl;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.List;

final class ConditionBuilderImpl implements ConditionBuilder {
    String mergeStrategy = null;
    SlotTypeImpl.Condition condition = null;

    @Override
    public ConditionBuilder mergeWithAnd() {
        this.mergeStrategy = "and";
        return this;
    }

    @Override
    public ConditionBuilder mergeWithOr() {
        this.mergeStrategy = "or";
        return this;
    }

    @Override
    public ConditionBuilder replace() {
        this.mergeStrategy = "replace";
        return this;
    }

    @Override
    public ConditionBuilder andAllOf(Identifier... identifier) {
        var con = new SlotTypeImpl.AndCondition(asCondition(identifier));
        if (this.condition == null) {
            this.condition = con;
        } else {
            this.condition = new SlotTypeImpl.AndCondition(List.of(this.condition, con));
        }
        return this;
    }

    @Override
    public ConditionBuilder andAnyOf(Identifier... identifier) {
        var con = new SlotTypeImpl.OrCondition(asCondition(identifier));
        if (this.condition == null) {
            this.condition = con;
        } else {
            this.condition = new SlotTypeImpl.AndCondition(List.of(this.condition, con));
        }
        return this;
    }

    @Override
    public ConditionBuilder orAllOf(Identifier... identifier) {
        var con = new SlotTypeImpl.AndCondition(asCondition(identifier));
        if (this.condition == null) {
            this.condition = con;
        } else {
            this.condition = new SlotTypeImpl.OrCondition(List.of(this.condition, con));
        }
        return this;
    }

    @Override
    public ConditionBuilder orAnyOf(Identifier... identifier) {
        var con = new SlotTypeImpl.OrCondition(asCondition(identifier));
        if (this.condition == null) {
            this.condition = con;
        } else {
            this.condition = new SlotTypeImpl.OrCondition(List.of(this.condition, con));
        }
        return this;
    }

    private List<SlotTypeImpl.Condition> asCondition(Identifier[] identifier) {
        return List.copyOf(Arrays.stream(identifier).map(SlotTypeImpl.DirectCondition::new).toList());
    }
}