package eu.pb4.trinkets.api.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
@ApiStatus.NonExtendable
public interface TrinketRenderContext {
    Type type();

    void setupAttachedRenderer(AttachmentSettings settings, SubmitCall call);
    void overrideEquipment(EquipmentSlot equipmentSlot, TrinketSlotAccess access, ItemStack item, boolean force, @Nullable ResourceKey<EquipmentAsset> assetId);
    void overrideEquipment(EquipmentSlot equipmentSlot, TrinketSlotAccess access, ItemStack item, boolean force, @Nullable EquipmentClientInfo override);
    void wingsOverride(TrinketSlotAccess access, ItemStack item, boolean force, @Nullable ResourceKey<EquipmentAsset> assetId);
    void wingsOverride(TrinketSlotAccess access, ItemStack item, boolean force, @Nullable EquipmentClientInfo override);

    Minecraft minecraft();
    ItemModelResolver itemModelResolver();
    BlockModelResolver blockModelResolver();

    interface SubmitCall {
        void submit(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final int overlayCoords, final int outlineColor);
    }

    enum Type {
        FULL,
        ARM
    }
}
