package eu.pb4.trinkets.api.component;

import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.impl.component.TrinketEquippableImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@ApiStatus.NonExtendable
public interface TrinketEquippable {
    TrinketEquippable DEFAULT = TrinketEquippableImpl.DEFAULT;

    boolean canBeEquippedBy(LivingEntity entity);

    TrinketEquippable withSlots(String... slots);

    TrinketEquippable withEquipSound(Holder<SoundEvent> equipSound);

    TrinketEquippable withAllowedEntities(@Nullable HolderSet<EntityType<?>> allowedEntities);

    TrinketEquippable withAssetId(@Nullable Identifier assetId);

    TrinketEquippable withDropRule(TrinketDropRule dropRule);

    TrinketEquippable withSwappable(boolean swappable);

    TrinketEquippable withEquipOnInteract(boolean equipOnInteract);

    List<String> allowedSlots();

    Holder<SoundEvent> equipSound();

    Optional<Identifier> assetId();

    Optional<HolderSet<EntityType<?>>> allowedEntities();

    TrinketDropRule dropRule();

    boolean swappable();

    boolean equipOnInteract();
}
