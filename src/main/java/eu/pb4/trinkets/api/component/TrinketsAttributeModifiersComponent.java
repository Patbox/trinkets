package eu.pb4.trinkets.api.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.apache.commons.lang3.function.TriConsumer;

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
            entry.provide(consumer, slot);
        }
    }

    public void forEach(TrinketSlotAccess slot, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer) {
        for (Entry entry : this.modifiers) {
            entry.provide(consumer, slot);
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

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, boolean unique) {
            return add(attribute, modifier, Optional.empty(), unique);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, String slot, boolean unique) {
            return add(attribute, modifier, Optional.of(slot), unique);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot) {
            return this.add(attribute, modifier, slot, true);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot, boolean unique) {
            return this.add(attribute, modifier, slot, ItemAttributeModifiers.Display.attributeModifiers(), unique);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, ItemAttributeModifiers.Display display) {
            return add(attribute, modifier, Optional.empty(), display);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, String slot, ItemAttributeModifiers.Display display) {
            return add(attribute, modifier, Optional.of(slot), display);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, ItemAttributeModifiers.Display display, boolean unique) {
            return add(attribute, modifier, Optional.empty(), display, unique);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, String slot, ItemAttributeModifiers.Display display, boolean unique) {
            return add(attribute, modifier, Optional.of(slot), display, unique);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot, ItemAttributeModifiers.Display display) {
            return this.add(attribute, modifier, slot, display, true);
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot, ItemAttributeModifiers.Display display, boolean unique) {
            this.entries.add(new Entry(attribute, modifier, slot, display, unique));
            return this;
        }

        public TrinketsAttributeModifiersComponent build() {
            return new TrinketsAttributeModifiersComponent(this.entries.build());
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot, ItemAttributeModifiers.Display display, boolean unique) {

        @Deprecated
        public Entry(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot) {
            this(attribute, modifier, slot, ItemAttributeModifiers.Display.attributeModifiers(), true);
        }

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("type").forGetter(Entry::attribute),
                AttributeModifier.MAP_CODEC.forGetter(Entry::modifier),
                Codec.STRING.optionalFieldOf("slot").forGetter(Entry::slot),
                ItemAttributeModifiers.Display.CODEC.optionalFieldOf("display", ItemAttributeModifiers.Display.attributeModifiers()).forGetter(Entry::display),
                Codec.BOOL.optionalFieldOf("unique", true).forGetter(Entry::unique)
        ).apply(instance, Entry::new));


        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE),
                Entry::attribute,
                AttributeModifier.STREAM_CODEC,
                Entry::modifier,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
                Entry::slot,
                ItemAttributeModifiers.Display.STREAM_CODEC,
                Entry::display,
                ByteBufCodecs.BOOL,
                Entry::unique,
                Entry::new);

        public void provide(BiConsumer<Holder<Attribute>, AttributeModifier> consumer, TrinketSlotAccess slot) {
            if (this.slot.isEmpty() || this.slot.get().equals(slot.slotType().getId())) {
                if (this.unique) {
                    consumer.accept(this.attribute, new AttributeModifier(this.modifier.id().withSuffix("/" + slot.getAsIdentifierPath()), this.modifier.amount(), this.modifier.operation()));
                } else {
                    consumer.accept(this.attribute, this.modifier);
                }
            }
        }

        public void provide(TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer, TrinketSlotAccess slot) {
            if (this.slot.isEmpty() || this.slot.get().equals(slot.slotType().getId())) {
                if (this.unique) {
                    consumer.accept(this.attribute, new AttributeModifier(this.modifier.id().withSuffix("/" + slot.getAsIdentifierPath()), this.modifier.amount(), this.modifier.operation()), this.display);
                } else {
                    consumer.accept(this.attribute, this.modifier, this.display);
                }
            }
        }
    }
}