package eu.pb4.trinkets.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void clearTrinkets(CallbackInfo ci) {
        LivingEntityTrinketAttachment.get(this.player).clearContents();
    }

    @ModifyReturnValue(method = "clearOrCountMatchingItems", at = @At("TAIL"))
    private int clearOrCountTrinkets(int count, @Local(argsOnly = true) Predicate<ItemStack> predicate, @Local(argsOnly = true) int amountToRemove) {
        var countingOnly = amountToRemove == 0;

        for (var x : LivingEntityTrinketAttachment.get(this.player).inventory.values()) {
            count += ContainerHelper.clearOrCountMatchingItems(x, predicate, amountToRemove - count, countingOnly);
        }
        return count;
    }

    @ModifyReturnValue(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("TAIL"))
    private boolean containsTrinkets(boolean original, @Local(argsOnly = true) TagKey<Item> tagKey) {
        return original || LivingEntityTrinketAttachment.get(this.player).isEquipped(tagKey);
    }

    @ModifyReturnValue(method = "contains(Ljava/util/function/Predicate;)Z", at = @At("TAIL"))
    private boolean containsTrinkets(boolean original, @Local(argsOnly = true) Predicate<ItemStack> predicate) {
        return original || LivingEntityTrinketAttachment.get(this.player).isEquipped(predicate);
    }

    @ModifyReturnValue(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("TAIL"))
    private boolean containsTrinkets(boolean original, @Local(argsOnly = true) ItemStack itemStack) {
        return original || LivingEntityTrinketAttachment.get(this.player).isEquipped(x -> ItemStack.isSameItemSameComponents(x, itemStack));
    }
}