package eu.pb4.trinkets.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CapeLayer.class)
public class CapeLayerMixin {

    @Shadow
    @Final
    private EquipmentAssetManager equipmentAssets;

    @WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z"))
    private boolean modifyElytraAndEquipmentCheck(CapeLayer instance, ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Operation<Boolean> original, @Local(argsOnly = true) AvatarRenderState state) {
        if (layerType == EquipmentClientInfo.LayerType.WINGS) {
            var override = ((TrinketEntityRenderState) state).trinkets$getWingOverride();

            if (override != null && (original.call(instance, override.stack(), layerType)
                    || override.assetResourceKey().isPresent() && !this.equipmentAssets.get(override.assetResourceKey().get()).getLayers(layerType).isEmpty())) {
                return true;
            }
        }

        return original.call(instance, itemStack, layerType);
    }
}
