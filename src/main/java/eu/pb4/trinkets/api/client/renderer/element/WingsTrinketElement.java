package eu.pb4.trinkets.api.client.renderer.element;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.mixin.client.accessor.EquipmentClientInfoAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
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
    public MapCodec<? extends TrinketRenderElement> type() {
        return CODEC;
    }

    @Override
    public Baked bake(BakingContext bakingContext) {
        return (owner, item, access, level, context, state) -> {
            if (this.asset.isPresent()) {
                var assetId = this.asset.get().left();
                if (assetId.isEmpty()) {
                    context.wingsOverride(access, item, this.force, this.asset.get().right().orElseThrow());
                }
                context.wingsOverride(access, item, this.force, assetId.orElseThrow());
            } else {
                context.wingsOverride(access, item, this.force, (ResourceKey<EquipmentAsset>) null);
            }
        };
    }
}
