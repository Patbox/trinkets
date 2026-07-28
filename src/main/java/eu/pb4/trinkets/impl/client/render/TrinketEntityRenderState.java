package eu.pb4.trinkets.impl.client.render;

import eu.pb4.trinkets.api.TrinketSlotAccess;

import java.util.List;
import java.util.Optional;

import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.renderer.AttachmentSettings;
import eu.pb4.trinkets.api.client.renderer.TrinketRenderContext;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;

public interface TrinketEntityRenderState {
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

    ;

    record EquipmentOverride(TrinketSlotAccess access, ItemStack stack,
                             boolean force,
                             Optional<ResourceKey<EquipmentAsset>> assetResourceKey,
                             Optional<EquipmentClientInfo> override) {
    }

    record PartAttachedRenderer(AttachmentSettings settings, TrinketRenderContext.SubmitCall call) {}

    record CodeRenderCall(TrinketSlotAccess access, ItemStack itemStack, TrinketRenderer renderer) {}
}
