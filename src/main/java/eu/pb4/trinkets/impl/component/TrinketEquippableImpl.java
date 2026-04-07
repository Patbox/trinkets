package eu.pb4.trinkets.impl.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.component.TrinketEquippable;
import eu.pb4.trinkets.impl.TrinketUtilities;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record TrinketEquippableImpl(
        List<String> allowedSlots,
        Holder<SoundEvent> equipSound,
        Optional<ResourceKey<EquipmentAsset>> assetId,
        Optional<HolderSet<EntityType<?>>> allowedEntities,
        TrinketDropRule dropRule,
        boolean swappable,
        boolean equipOnInteract
) implements TrinketEquippable {
    public static final TrinketEquippable DEFAULT = new TrinketEquippableImpl(List.of(), SoundEvents.ARMOR_EQUIP_GENERIC, Optional.empty(), Optional.empty(), TrinketDropRule.DEFAULT, false,  true);

    public static final Codec<TrinketEquippable> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ExtraCodecs.compactListCodec(Codec.STRING).fieldOf("slot").forGetter(TrinketEquippable::allowedSlots),
            SoundEvent.CODEC.optionalFieldOf("equip_sound", SoundEvents.ARMOR_EQUIP_GENERIC).forGetter(TrinketEquippable::equipSound),
            ResourceKey.codec(EquipmentAssets.ROOT_ID).optionalFieldOf("asset_id").forGetter(TrinketEquippable::assetId),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("allowed_entities").forGetter(TrinketEquippable::allowedEntities),
            StringRepresentable.fromEnum(TrinketDropRule::values).optionalFieldOf("drop_rule", TrinketDropRule.DEFAULT).forGetter(TrinketEquippable::dropRule),
            Codec.BOOL.optionalFieldOf("swappable", true).forGetter(TrinketEquippable::swappable),
            Codec.BOOL.optionalFieldOf("equip_on_interact", false).forGetter(TrinketEquippable::equipOnInteract)
    ).apply(instance, TrinketEquippableImpl::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrinketEquippable> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TrinketEquippable::allowedSlots,
            SoundEvent.STREAM_CODEC, TrinketEquippable::equipSound,
            ResourceKey.streamCodec(EquipmentAssets.ROOT_ID).apply(ByteBufCodecs::optional), TrinketEquippable::assetId,
            ByteBufCodecs.holderSet(Registries.ENTITY_TYPE).apply(ByteBufCodecs::optional), TrinketEquippable::allowedEntities,
            ByteBufCodecs.idMapper(x -> TrinketDropRule.values()[x], TrinketDropRule::ordinal), TrinketEquippable::dropRule,
            ByteBufCodecs.BOOL, TrinketEquippable::swappable,
            ByteBufCodecs.BOOL, TrinketEquippable::equipOnInteract,
            TrinketEquippableImpl::new
    );

    @Override
    public boolean canBeEquippedBy(LivingEntity entity) {
        return (this.allowedEntities.isEmpty() || this.allowedEntities.get().contains(entity.typeHolder())) && TrinketUtilities.hasOneOfSlots(entity, this.allowedSlots);
    }

    @Override
    public TrinketEquippable withSlots(String... slots) {
        return new TrinketEquippableImpl(List.of(slots), equipSound, assetId, allowedEntities, dropRule, swappable, equipOnInteract);
        //return new TrinketEquippable(slot, equipSound, assetId, allowedEntities, dropRule, swappable, equipOnInteract);
    }

    @Override
    public TrinketEquippable withEquipSound(Holder<SoundEvent> equipSound) {
        return new TrinketEquippableImpl(allowedSlots, equipSound, assetId, allowedEntities, dropRule, swappable, equipOnInteract);
    }

    @Override
    public TrinketEquippable withAllowedEntities(@Nullable HolderSet<EntityType<?>> allowedEntities) {
        return new TrinketEquippableImpl(allowedSlots, equipSound, assetId, Optional.ofNullable(allowedEntities), dropRule, swappable, equipOnInteract);
    }

    @Override
    public TrinketEquippable withDropRule(TrinketDropRule dropRule) {
        return new TrinketEquippableImpl(allowedSlots, equipSound, assetId, allowedEntities, dropRule, swappable, equipOnInteract);
    }

    @Override
    public TrinketEquippable withSwappable(boolean swappable) {
        return new TrinketEquippableImpl(allowedSlots, equipSound, assetId, allowedEntities, dropRule, swappable, equipOnInteract);
    }

    @Override
    public TrinketEquippable withEquipOnInteract(boolean equipOnInteract) {
        return new TrinketEquippableImpl(allowedSlots, equipSound, assetId, allowedEntities, dropRule, swappable, equipOnInteract);
    }

    //@Override
    public TrinketEquippable withAssetId(@Nullable ResourceKey<EquipmentAsset> assetId) {
        return new TrinketEquippableImpl(allowedSlots, equipSound, Optional.ofNullable(assetId), allowedEntities, dropRule, swappable, equipOnInteract);
    }
}