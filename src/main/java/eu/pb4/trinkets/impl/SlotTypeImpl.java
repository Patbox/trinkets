package eu.pb4.trinkets.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SlotTypeImpl(String group, String name, int order, int amount, Optional<Identifier> optionalIcon,
                           Condition quickMovePredicates, Condition validatorPredicates,
                           Condition tooltipPredicates, TrinketDropRule dropRule) implements SlotType {

    public static StreamCodec<FriendlyByteBuf, SlotTypeImpl> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SlotTypeImpl::group,
            ByteBufCodecs.STRING_UTF8, SlotTypeImpl::name,
            ByteBufCodecs.INT, SlotTypeImpl::order,
            ByteBufCodecs.INT, SlotTypeImpl::amount,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), SlotTypeImpl::optionalIcon,
            Condition.STREAM_CODEC, SlotTypeImpl::quickMovePredicates,
            Condition.STREAM_CODEC, SlotTypeImpl::validatorPredicates,
            Condition.STREAM_CODEC, SlotTypeImpl::tooltipPredicates,
            ByteBufCodecs.idMapper(x -> TrinketDropRule.values()[x], TrinketDropRule::ordinal), SlotTypeImpl::dropRule,
            SlotTypeImpl::new
    );

    @Override
    public MutableComponent getTranslation() {
        return Component.translatable("trinkets.slot." + this.group + "." + this.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlotType slotType = (SlotType) o;
        return group.equals(slotType.group()) && name.equals(slotType.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name);
    }

    @Override
    public String getId() {
        return this.group + "/" + this.name;
    }

    @Override
    public Identifier icon() {
        return this.optionalIcon.orElse(null);
    }

    @Override
    public boolean quickMoveCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
        return this.quickMovePredicates.test(stack, slotRef, entity);
    }

    @Override
    public boolean validatorCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
        return this.validatorPredicates.test(stack, slotRef, entity);
    }

    @Override
    public boolean tooltipCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
        return this.tooltipPredicates.test(stack, slotRef, entity);
    }

    public interface Condition {
        StreamCodec<FriendlyByteBuf, Condition> STREAM_CODEC = new StreamCodec<>() {
            final StreamCodec<FriendlyByteBuf, DirectCondition> direct = Identifier.STREAM_CODEC.map(DirectCondition::new, DirectCondition::identifier).cast();
            final StreamCodec<FriendlyByteBuf, AndCondition> and = this.apply(ByteBufCodecs.list()).map(AndCondition::new, AndCondition::conditions);
            final StreamCodec<FriendlyByteBuf, OrCondition> or = this.apply(ByteBufCodecs.list()).map(OrCondition::new, OrCondition::conditions);
            final StreamCodec<FriendlyByteBuf, ConstantCondition> constant = ByteBufCodecs.BOOL.map(ConstantCondition::new, ConstantCondition::value).cast();

            @Override
            public Condition decode(FriendlyByteBuf input) {
                var id = input.readByte();

                return switch (id) {
                    case 0 -> direct.decode(input);
                    case 1 -> and.decode(input);
                    case 2 -> or.decode(input);
                    case 3 -> constant.decode(input);
                    default -> throw new IllegalStateException("Unsupported Trinket Condition type");
                };
            }

            @Override
            public void encode(FriendlyByteBuf output, Condition value) {
                if (value instanceof DirectCondition val) {
                    output.writeByte(0);
                    direct.encode(output, val);
                } else if (value instanceof AndCondition val) {
                    output.writeByte(1);
                    and.encode(output, val);
                } else if (value instanceof OrCondition val) {
                    output.writeByte(2);
                    or.encode(output, val);
                } else if (value instanceof ConstantCondition val) {
                    output.writeByte(3);
                    constant.encode(output, val);
                } else {
                    throw new IllegalStateException("Unsupported Trinket Condition type");
                }
            }
        };

        Codec<Condition> CODEC = new Codec<>() {
            final Codec<List<Condition>> listCodec = ExtraCodecs.compactListCodec(this);

            @Override
            public <T> DataResult<Pair<Condition, T>> decode(DynamicOps<T> ops, T input) {
                var bool = Codec.BOOL.decode(ops, input);
                if (bool.isSuccess()) {
                    return bool.map(x -> x.mapFirst(ConstantCondition::new));
                }

                var maybeDirect = Identifier.CODEC.decode(ops, input);
                if (maybeDirect.isSuccess()) {
                    return maybeDirect.map(x -> x.mapFirst(DirectCondition::new));
                }
                var and = ops.get(input, "and");
                if (and.isSuccess()) {
                    return and.flatMap(x -> listCodec.decode(ops, x)).map(x -> x.mapFirst(AndCondition::new));
                }

                var or = ops.get(input, "or");
                if (or.isSuccess()) {
                    return and.flatMap(x -> listCodec.decode(ops, x)).map(x -> x.mapFirst(OrCondition::new));
                }

                return listCodec.map(OrCondition::new).map(x -> (Condition) x).decode(ops, input);
            }

            @Override
            public <T> DataResult<T> encode(Condition value, DynamicOps<T> ops, T prefix) {
                if (value instanceof DirectCondition(Identifier identifier)) {
                    return Identifier.CODEC.encode(identifier, ops, prefix);
                } else if (value instanceof AndCondition(List<Condition> conditions1)) {
                    return listCodec.encodeStart(ops, conditions1).flatMap(x -> ops.mergeToMap(prefix, ops.createString("and"), x));
                } else if (value instanceof OrCondition(List<Condition> conditions)) {
                    return listCodec.encodeStart(ops, conditions).flatMap(x -> ops.mergeToMap(prefix, ops.createString("or"), x));
                } else if (value instanceof ConstantCondition(boolean val)) {
                    return Codec.BOOL.encode(val, ops, prefix);
                } else {
                    return DataResult.error(() -> "Unsupported Trinket Condition type");
                }
            }
        };

        boolean test(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);

        boolean isEmpty();
    }

    public record DirectCondition(Identifier identifier) implements Condition {
        public boolean test(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
            var x = TrinketsApi.getTrinketPredicate(identifier);
            return x != null && x.test(stack, slotRef, entity);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public record AndCondition(List<Condition> conditions) implements Condition {
        @Override
        public boolean test(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
            for (var c : conditions) {
                if (!c.test(stack, slotRef, entity)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return this.conditions.isEmpty();
        }
    }

    public record OrCondition(List<Condition> conditions) implements Condition {
        @Override
        public boolean test(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
            for (var c : conditions) {
                if (c.test(stack, slotRef, entity)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return this.conditions.isEmpty();
        }
    }

    public record ConstantCondition(boolean value) implements Condition {
        @Override
        public boolean test(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
            return value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
