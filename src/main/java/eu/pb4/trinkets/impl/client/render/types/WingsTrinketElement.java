package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.TrinketRenderState;
import eu.pb4.trinkets.impl.client.render.ClientRenderPasshack;
import eu.pb4.trinkets.mixin.client.EquipmentClientInfoAccessor;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;
import java.util.Optional;

public record WingsTrinketElement(Optional<Either<ResourceKey<EquipmentAsset>, EquipmentClientInfo>> asset,
                                  boolean force) implements TrinketRenderElement {
    public static final MapCodec<WingsTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(ResourceKey.codec(EquipmentAssets.ROOT_ID),
                    EquipmentClientInfoAccessor.getLAYER_LIST_CODEC()
                            .xmap(x -> new EquipmentClientInfo(Map.of(EquipmentClientInfo.LayerType.WINGS, x)),
                                    x -> x.getLayers(EquipmentClientInfo.LayerType.WINGS)
                            )
            ).optionalFieldOf("asset").forGetter(WingsTrinketElement::asset),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(WingsTrinketElement::force)
    ).apply(instance, WingsTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketRenderState state) {
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

        state.trinkets$setWingOverride(new TrinketRenderState.EquipmentOverride(access, stack, this.force, assetId, override));
    }
}
