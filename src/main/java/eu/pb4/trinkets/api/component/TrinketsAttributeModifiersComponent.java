package eu.pb4.trinkets.api.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public record TrinketsAttributeModifiersComponent(List<Entry> modifiers) {
    public static final TrinketsAttributeModifiersComponent DEFAULT = new TrinketsAttributeModifiersComponent(List.of());
    static final StreamCodec<RegistryFriendlyByteBuf, TrinketsAttributeModifiersComponent> PACKET_CODEC = StreamCodec.composite(
            Entry.PACKET_CODEC.apply(ByteBufCodecs.list()),
            TrinketsAttributeModifiersComponent::modifiers,
            TrinketsAttributeModifiersComponent::new);
    private static final Codec<TrinketsAttributeModifiersComponent> LEGACY_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(
                Entry.CODEC.listOf().fieldOf("modifiers").forGetter(TrinketsAttributeModifiersComponent::modifiers)
        ).apply(instance, TrinketsAttributeModifiersComponent::new);
    });
    static final Codec<TrinketsAttributeModifiersComponent> CODEC = Codec.withAlternative(
            Entry.CODEC.listOf().xmap(TrinketsAttributeModifiersComponent::new, TrinketsAttributeModifiersComponent::modifiers),
            LEGACY_CODEC);

    public static Builder builder() {
        return new Builder();
    }

    public List<Entry> modifiers() {
        return this.modifiers;
    }

    public void forEach(TrinketSlotAccess slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        for (Entry entry : this.modifiers) {
            if (entry.slot.isEmpty() || entry.slot.get().equals(slot.getSerializedName())) {
                consumer.accept(entry.attribute, entry.modifier);
            }
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<Entry> entries = ImmutableList.builder();

        Builder() {
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier) {
            return add(attribute, modifier, Optional.empty());
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, String slot) {
            return add(attribute, modifier, Optional.of(slot));
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot) {
            this.entries.add(new Entry(attribute, modifier, slot));
            return this;
        }

        public TrinketsAttributeModifiersComponent build() {
            return new TrinketsAttributeModifiersComponent(this.entries.build());
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("type").forGetter(Entry::attribute),
                AttributeModifier.MAP_CODEC.forGetter(Entry::modifier),
                Codec.STRING.optionalFieldOf("slot").forGetter(Entry::slot)
        ).apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE),
                Entry::attribute,
                AttributeModifier.STREAM_CODEC,
                Entry::modifier,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
                Entry::slot,
                Entry::new);
    }
}