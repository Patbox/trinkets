package eu.pb4.trinkets.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;

import java.util.List;
import java.util.Optional;

import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.impl.client.render.types.AttachmentSettings;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;

public interface TrinketRenderState {
    void trinkets$setCodeRenderers(List<CodeRenderCall> items);

    void trinkets$setPartAttachedRenderers(List<PartAttachedRenderer> items);

    List<CodeRenderCall> trinkets$getCodeRenderers();

    List<PartAttachedRenderer> trinkets$getPartAttachedRenderers();

    void trinkets$setEquipmentOverride(EquipmentSlot slot, EquipmentOverride override);

    @Nullable
    EquipmentOverride trinkets$getEquipmentOverride(EquipmentSlot slot);

    void trinkets$setWingOverride(EquipmentOverride override);

    @Nullable
    EquipmentOverride trinkets$getWingOverride();


    interface SubmitCall {
        void submit(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final int overlayCoords, final int outlineColor);
    }

    ;

    record EquipmentOverride(TrinketSlotAccess access, ItemStack stack,
                             boolean force,
                             Optional<ResourceKey<EquipmentAsset>> assetResourceKey,
                             Optional<EquipmentClientInfo> override) {
    }

    record PartAttachedRenderer(AttachmentSettings settings, SubmitCall call) {
    }

    record CodeRenderCall(TrinketSlotAccess access, ItemStack itemStack, TrinketRenderer renderer) {
    }
}
