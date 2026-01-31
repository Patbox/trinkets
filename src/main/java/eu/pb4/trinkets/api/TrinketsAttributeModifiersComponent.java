package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
	public static final TrinketsAttributeModifiersComponent DEFAULT = new TrinketsAttributeModifiersComponent(List.of(), true);
	private static final Codec<TrinketsAttributeModifiersComponent> BASE_CODEC = RecordCodecBuilder.create((instance) -> {
		return instance.group(
						Entry.CODEC.listOf().fieldOf("modifiers").forGetter(TrinketsAttributeModifiersComponent::modifiers),
						Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(TrinketsAttributeModifiersComponent::showInTooltip)
				).apply(instance, TrinketsAttributeModifiersComponent::new);
	});
	public static final Codec<TrinketsAttributeModifiersComponent> CODEC = Codec.withAlternative(BASE_CODEC, Entry.CODEC.listOf(), (attributeModifiers) -> {
		return new TrinketsAttributeModifiersComponent(attributeModifiers, true);
	});

	public static final StreamCodec<RegistryFriendlyByteBuf, TrinketsAttributeModifiersComponent> PACKET_CODEC = StreamCodec.composite(
			Entry.PACKET_CODEC.apply(ByteBufCodecs.list()),
			TrinketsAttributeModifiersComponent::modifiers,
			ByteBufCodecs.BOOL,
			TrinketsAttributeModifiersComponent::showInTooltip,
			TrinketsAttributeModifiersComponent::new);

	public static final DataComponentType<TrinketsAttributeModifiersComponent> TYPE = DataComponentType.<TrinketsAttributeModifiersComponent>builder().persistent(CODEC).networkSynchronized(PACKET_CODEC).build();

	public TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
		this.modifiers = modifiers;
		this.showInTooltip = showInTooltip;
	}

	public TrinketsAttributeModifiersComponent withShowInTooltip(boolean showInTooltip) {
		return new TrinketsAttributeModifiersComponent(this.modifiers, showInTooltip);
	}

	public List<Entry> modifiers() {
		return this.modifiers;
	}

	public boolean showInTooltip() {
		return this.showInTooltip;
	}

	public static Builder builder() {
		return new Builder();
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
			this.entries.add(new Entry (attribute, modifier, slot));
			return this;
		}

		public TrinketsAttributeModifiersComponent build() {
			return new TrinketsAttributeModifiersComponent(this.entries.build(), true);
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