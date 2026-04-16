package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.TrinketEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Optional;

public record WingsTrinketElement(Optional<ResourceKey<EquipmentAsset>> assetId,
                                  boolean force) implements TrinketRenderElement {
    public static final MapCodec<WingsTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(EquipmentAssets.ROOT_ID).optionalFieldOf("asset_id").forGetter(WingsTrinketElement::assetId),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(WingsTrinketElement::force)
    ).apply(instance, WingsTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        state.trinkets$setWingOverride(new TrinketEntityRenderState.EquipmentOverride(access, stack, this.force, assetId));
    }
}
