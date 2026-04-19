package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.client.render.ClientRenderPasshack;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Optional;

public record EquipmentReplaceTrinketElement(Optional<Either<ResourceKey<EquipmentAsset>, EquipmentClientInfo>> asset, Optional<EquipmentSlot> equipmentSlot) implements TrinketRenderElement {
    public static final MapCodec<EquipmentReplaceTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(ResourceKey.codec(EquipmentAssets.ROOT_ID), EquipmentClientInfo.CODEC).optionalFieldOf("asset").forGetter(EquipmentReplaceTrinketElement::asset),
            EquipmentSlot.CODEC.optionalFieldOf("equipment_slot").forGetter(EquipmentReplaceTrinketElement::equipmentSlot)
    ).apply(instance, EquipmentReplaceTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);

        var equipmentSlot = this.equipmentSlot.orElse(equippable != null ? equippable.slot() : EquipmentSlot.BODY);
        Optional<ResourceKey<EquipmentAsset>> assetId;
        Optional<EquipmentClientInfo> override;

        if (this.asset.isPresent()) {
            assetId = this.asset.get().left();
            if (assetId.isEmpty()) {
                assetId = ClientRenderPasshack.FAKE_ASSET_OPT;
            }
            override = this.asset.get().right();
        } else {
            assetId = Optional.empty();
            override = Optional.empty();
        }

        state.trinkets$setEquipmentOverride(equipmentSlot, new TrinketEntityRenderState.EquipmentOverride(access, stack, true, assetId, override));
    }
}
