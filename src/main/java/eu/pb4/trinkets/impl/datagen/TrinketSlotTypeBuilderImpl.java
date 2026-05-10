package eu.pb4.trinkets.impl.datagen;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.datagen.ConditionBuilder;
import eu.pb4.trinkets.api.datagen.TrinketSlotTypeBuilder;
import eu.pb4.trinkets.impl.SlotTypeImpl;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public final class TrinketSlotTypeBuilderImpl implements TrinketSlotTypeBuilder {
    private Boolean replace = null;
    private Integer order = null;
    private Integer amount = null;
    private Integer maxStackSize = null;
    private Identifier icon = null;
    private ConditionBuilderImpl quickMove = null;
    private ConditionBuilderImpl validator = null;
    private ConditionBuilderImpl tooltip = null;
    private ConditionBuilderImpl interactEquipable = null;
    private TrinketDropRule dropRule = null;
    private Boolean isVanityOnly = null;
    private Boolean isHidden = null;

    @Override
    public TrinketSlotTypeBuilder replace(boolean replace) {
        this.replace = replace;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder order(int order) {
        this.order = order;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder amount(int defaultAmount) {
        this.amount = defaultAmount;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder icon(Identifier icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder dropRule(TrinketDropRule dropRule) {
        this.dropRule = dropRule;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder isVanityOnly(boolean value) {
        this.isVanityOnly = value;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder isHidden(boolean value) {
        this.isHidden = value;
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder quickMoveCondition(Consumer<ConditionBuilder> consumer) {
        if (this.quickMove == null) {
            this.quickMove = new ConditionBuilderImpl();
        }
        consumer.accept(this.quickMove);
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder validatorCondition(Consumer<ConditionBuilder> consumer) {
        if (this.validator == null) {
            this.validator = new ConditionBuilderImpl();
        }
        consumer.accept(this.validator);
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder tooltipCondition(Consumer<ConditionBuilder> consumer) {
        if (this.tooltip == null) {
            this.tooltip = new ConditionBuilderImpl();
        }
        consumer.accept(this.tooltip);
        return this;
    }

    @Override
    public TrinketSlotTypeBuilder interactEquipableCondition(Consumer<ConditionBuilder> consumer) {
        if (this.interactEquipable == null) {
            this.interactEquipable = new ConditionBuilderImpl();
        }
        consumer.accept(this.interactEquipable);
        return this;
    }


    public JsonObject toJson() {
        var object = new JsonObject();

        if (this.replace != null) {
            object.addProperty("replace", this.replace);
        }
        if (this.order != null) {
            object.addProperty("order", this.order);
        }
        if (this.amount != null) {
            object.addProperty("amount", this.amount);
        }
        if (this.maxStackSize != null) {
            object.addProperty("max_stack_size", this.maxStackSize);
        }
        if (this.icon != null) {
            object.addProperty("icon", this.icon.toString());
        }
        if (this.dropRule != null) {
            object.addProperty("drop_rule", this.dropRule.getSerializedName());
        }
        if (this.isVanityOnly != null) {
            object.addProperty("is_vanity", this.isVanityOnly);
        }
        if (this.isHidden != null) {
            object.addProperty("is_hidden", this.isHidden);
        }

        this.writeCondition(object, "quick_move_predicates", this.quickMove);
        this.writeCondition(object, "validator_predicates", this.validator);
        this.writeCondition(object, "tooltip_predicates", this.tooltip);
        this.writeCondition(object, "interact_equipable_predicates", this.interactEquipable);

        return object;
    }

    private void writeCondition(JsonObject object, String type, ConditionBuilderImpl builder) {
        if (builder == null || builder.condition == null) {
            return;
        }

        if (builder.mergeStrategy != null) {
            object.addProperty(type + ":merge_type", builder.mergeStrategy);
        }

        object.add(type, SlotTypeImpl.Condition.CODEC.encodeStart(JsonOps.INSTANCE, builder.condition).getOrThrow());
    }
}
