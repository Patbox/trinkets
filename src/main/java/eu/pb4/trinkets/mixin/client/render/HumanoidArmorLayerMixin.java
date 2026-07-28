package eu.pb4.trinkets.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.client.render.ClientRenderPasshack;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {
    @ModifyVariable(method = "renderArmorPiece", at = @At("HEAD"), argsOnly = true)
    private ItemStack replaceStack(ItemStack itemStack, @Local(argsOnly = true) EquipmentSlot slot, @Local(argsOnly = true) HumanoidRenderState state) {
        var override = ((TrinketEntityRenderState) state).trinkets$getEquipmentOverride(slot);
        if (override == null) {
            return itemStack;
        }

        return override.stack();
    }

    @ModifyExpressionValue(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object replaceEquipment(Object object, @Local(argsOnly = true) EquipmentSlot slot, @Local(argsOnly = true) HumanoidRenderState state) {
        var override = ((TrinketEntityRenderState) state).trinkets$getEquipmentOverride(slot);
        if (override == null) {
            return object;
        }
        ClientRenderPasshack.replacementEquipmentInfo = override.override();

        return override.assetResourceKey().isPresent() ? new Equippable(slot, SoundEvents.ARMOR_EQUIP_GENERIC, override.assetResourceKey(),
                Optional.empty(), Optional.empty(), false, false, false, false, false,
                SoundEvents.ARMOR_EQUIP_GENERIC) : object;
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void clearReplacement(CallbackInfo ci) {
        ClientRenderPasshack.replacementEquipmentInfo = Optional.empty();
    }
}
