package eu.pb4.trinkets.api.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.callback.RegisteredTrinketCallback;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.impl.TrinketUtilities;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.List;
import java.util.Optional;

public record TrinketEquippable(
        List<String> slot,
        Holder<SoundEvent> equipSound,
        Optional<ResourceKey<EquipmentAsset>> assetId,
        Optional<Identifier> cameraOverlay,
        Optional<HolderSet<EntityType<?>>> allowedEntities,
        TrinketDropRule dropRule,
        boolean swappable,
        boolean damageOnHurt, boolean equipOnInteract
) {
    public static final TrinketEquippable DEFAULT = new TrinketEquippable(List.of(), SoundEvents.ARMOR_EQUIP_GENERIC, Optional.empty(), Optional.empty(),
            Optional.empty(), TrinketDropRule.DEFAULT, false, false, true);

    public static final Codec<TrinketEquippable> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ExtraCodecs.compactListCodec(Codec.STRING).fieldOf("slot").forGetter(TrinketEquippable::slot),
            SoundEvent.CODEC.optionalFieldOf("equip_sound", SoundEvents.ARMOR_EQUIP_GENERIC).forGetter(TrinketEquippable::equipSound),
            ResourceKey.codec(EquipmentAssets.ROOT_ID).optionalFieldOf("asset_id").forGetter(TrinketEquippable::assetId),
            Identifier.CODEC.optionalFieldOf("camera_overlay").forGetter(TrinketEquippable::cameraOverlay),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("allowed_entities").forGetter(TrinketEquippable::allowedEntities),
            StringRepresentable.fromEnum(TrinketDropRule::values).optionalFieldOf("drop_rule", TrinketDropRule.DEFAULT).forGetter(TrinketEquippable::dropRule),
            Codec.BOOL.optionalFieldOf("swappable", true).forGetter(TrinketEquippable::swappable),
            Codec.BOOL.optionalFieldOf("damage_on_hurt", true).forGetter(TrinketEquippable::damageOnHurt),
            Codec.BOOL.optionalFieldOf("equip_on_interact", false).forGetter(TrinketEquippable::equipOnInteract)
    ).apply(instance, TrinketEquippable::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrinketEquippable> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TrinketEquippable::slot,
            SoundEvent.STREAM_CODEC, TrinketEquippable::equipSound,
            ResourceKey.streamCodec(EquipmentAssets.ROOT_ID).apply(ByteBufCodecs::optional), TrinketEquippable::assetId,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional), TrinketEquippable::cameraOverlay,
            ByteBufCodecs.holderSet(Registries.ENTITY_TYPE).apply(ByteBufCodecs::optional), TrinketEquippable::allowedEntities,
            ByteBufCodecs.idMapper(x -> TrinketDropRule.values()[x], TrinketDropRule::ordinal), TrinketEquippable::dropRule,
            ByteBufCodecs.BOOL, TrinketEquippable::swappable,
            ByteBufCodecs.BOOL, TrinketEquippable::damageOnHurt,
            ByteBufCodecs.BOOL, TrinketEquippable::equipOnInteract,
            TrinketEquippable::new
    );

    public boolean canBeEquippedBy(LivingEntity entity) {
        return (this.allowedEntities.isEmpty() || this.allowedEntities.get().contains(entity.typeHolder())) && TrinketUtilities.hasOneOfSlots(entity, this.slot);
    }
}