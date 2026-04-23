package eu.pb4.trinkets.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.client.render.TrinketRenderState;
import eu.pb4.trinkets.impl.client.render.ClientRenderPasshack;
import eu.pb4.trinkets.mixin.client.EquipmentLayerRendererAccessor;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(WingsLayer.class)
public class WingsLayerMixin {
    @Shadow
    @Final
    private EquipmentLayerRenderer equipmentRenderer;

    @ModifyExpressionValue(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;", opcode = Opcodes.GETFIELD))
    private ItemStack replaceWings(ItemStack original, @Local(argsOnly = true) HumanoidRenderState state) {
        var override = ((TrinketRenderState) state).trinkets$getWingOverride();
        if (override != null) {
            if (override.force() || original.isEmpty()) {
                return override.stack();
            }

            var equippable = original.get(DataComponents.EQUIPPABLE);
            if (equippable == null || equippable.assetId().isEmpty()
                    || ((EquipmentLayerRendererAccessor) this.equipmentRenderer).getEquipmentAssets().get(equippable.assetId().get())
                    .getLayers(EquipmentClientInfo.LayerType.WINGS).isEmpty()) {
                return override.stack();
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object replaceEquipment(Object object, @Local(argsOnly = true) HumanoidRenderState state, @Local ItemStack stack) {
        var override = ((TrinketRenderState) state).trinkets$getWingOverride();
        if (override == null || stack != override.stack()) {
            return object;
        }
        ClientRenderPasshack.replacementEquipmentInfo = override.override();

        return override.assetResourceKey().isPresent() ? new Equippable(EquipmentSlot.BODY, SoundEvents.ARMOR_EQUIP_GENERIC, override.assetResourceKey(),
                Optional.empty(), Optional.empty(), false, false, false, false, false,
                SoundEvents.ARMOR_EQUIP_GENERIC) : object;
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("RETURN"))
    private void clearReplacement(CallbackInfo ci) {
        ClientRenderPasshack.replacementEquipmentInfo = Optional.empty();
    }
}
