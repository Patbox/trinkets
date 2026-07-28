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

import java.util.function.Consumer;

public record TrinketRenderStateHandImpl(Minecraft minecraft,
                                         ItemModelResolver itemModelResolver,
                                         BlockModelResolver blockModelResolver,
                                         Consumer<TrinketEntityRenderState.PartAttachedRenderer> consumer) implements TrinketRenderContext {
    @Override
    public Type type() {
        return Type.ARM;
    }

    @Override
    public void setupAttachedRenderer(AttachmentSettings settings, SubmitCall call) {
        consumer.accept(new TrinketEntityRenderState.PartAttachedRenderer(settings, call));
    }

    @Override
    public void overrideEquipment(EquipmentSlot equipmentSlot, TrinketSlotAccess access, ItemStack item, boolean force, @Nullable ResourceKey<EquipmentAsset> assetId) {
    }
    @Override
    public void overrideEquipment(EquipmentSlot equipmentSlot, TrinketSlotAccess access, ItemStack item, boolean force, @Nullable EquipmentClientInfo override) {
    }

    @Override
    public void wingsOverride(TrinketSlotAccess access, ItemStack item, boolean force, @Nullable ResourceKey<EquipmentAsset> assetId) {
    }

    @Override
    public void wingsOverride(TrinketSlotAccess access, ItemStack item, boolean force, @Nullable EquipmentClientInfo override) {
    }
}
