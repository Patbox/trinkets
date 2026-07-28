package eu.pb4.trinkets.api.client.renderer.element;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public record EquipmentReplaceTrinketElement(Optional<Either<ResourceKey<EquipmentAsset>, EquipmentClientInfo>> asset, Optional<EquipmentSlot> equipmentSlot) implements TrinketRenderElement {
    public static final MapCodec<EquipmentReplaceTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(ResourceKey.codec(EquipmentAssets.ROOT_ID), EquipmentClientInfo.CODEC).optionalFieldOf("asset").forGetter(EquipmentReplaceTrinketElement::asset),
            EquipmentSlot.CODEC.optionalFieldOf("equipment_slot").forGetter(EquipmentReplaceTrinketElement::equipmentSlot)
    ).apply(instance, EquipmentReplaceTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> type() {
        return CODEC;
    }

    @Override
    public Baked bake(BakingContext context) {
        return (owner, item, access, level, trinketRenderState, state) -> {
            if (state == null) return;

            var equippable = item.get(DataComponents.EQUIPPABLE);

            var equipmentSlot = this.equipmentSlot.orElse(equippable != null ? equippable.slot() : EquipmentSlot.BODY);

            if (this.asset.isPresent()) {
                var assetId = this.asset.get().left();
                if (assetId.isEmpty()) {
                    trinketRenderState.overrideEquipment(equipmentSlot, access, item, true, this.asset.get().right().orElseThrow());
                } else {
                    trinketRenderState.overrideEquipment(equipmentSlot, access, item, true, assetId.orElseThrow());
                }
            } else {
                trinketRenderState.overrideEquipment(equipmentSlot, access, item, true, (ResourceKey<EquipmentAsset>) null);
            }
        };
    }
}
