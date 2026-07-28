package eu.pb4.trinkets.impl.client.render;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.renderer.AttachmentSettings;
import eu.pb4.trinkets.api.client.renderer.TrinketRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record TrinketRenderStateFullImpl(Minecraft minecraft,
                                         ItemModelResolver itemModelResolver,
                                         BlockModelResolver blockModelResolver,
                                         TrinketEntityRenderState state) implements TrinketRenderContext {
    @Override
    public Type type() {
        return Type.FULL;
    }

    @Override
    public void setupAttachedRenderer(AttachmentSettings settings, SubmitCall call) {
        state.trinkets$getPartAttachedRenderers().add(new TrinketEntityRenderState.PartAttachedRenderer(settings, call));
    }

    @Override
    public void overrideEquipment(EquipmentSlot equipmentSlot, TrinketSlotAccess access, ItemStack item, boolean force, @Nullable ResourceKey<EquipmentAsset> assetId) {
        state.trinkets$setEquipmentOverride(equipmentSlot, new TrinketEntityRenderState.EquipmentOverride(access, item, force,
                Optional.ofNullable(assetId), Optional.empty()));
    }
    @Override
    public void overrideEquipment(EquipmentSlot equipmentSlot, TrinketSlotAccess access, ItemStack item, boolean force, @Nullable EquipmentClientInfo override) {
        state.trinkets$setEquipmentOverride(equipmentSlot, new TrinketEntityRenderState.EquipmentOverride(access, item, force,
                override != null ? ClientRenderPasshack.FAKE_ASSET_OPT : Optional.empty(), Optional.ofNullable(override)));
    }

    @Override
    public void wingsOverride(TrinketSlotAccess access, ItemStack item, boolean force, @Nullable ResourceKey<EquipmentAsset> assetId) {
        state.trinkets$setWingOverride(new TrinketEntityRenderState.EquipmentOverride(access, item, force,
                Optional.ofNullable(assetId), Optional.empty()));
    }

    @Override
    public void wingsOverride(TrinketSlotAccess access, ItemStack item, boolean force, @Nullable EquipmentClientInfo override) {
        state.trinkets$setWingOverride(new TrinketEntityRenderState.EquipmentOverride(access, item, force,
                override != null ? ClientRenderPasshack.FAKE_ASSET_OPT : Optional.empty(), Optional.ofNullable(override)));
    }
}
