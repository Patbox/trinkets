package eu.pb4.trinkets.mixin.neo;

import eu.pb4.trinkets.api.ext.TrinketsItemStackExtension;
import eu.pb4.trinkets.impl.ItemStackTooltipUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Adds a tooltip for trinkets describing slots and attributes
 *
 * @author Emi
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements TrinketsItemStackExtension {
    @SuppressWarnings("MixinAnnotationTarget")
    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/util/AttributeUtil;addAttributeTooltips(Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/neoforged/neoforge/common/util/AttributeTooltipContext;)V",
            shift = Shift.BEFORE), require = 0)
    private void getTooltipNeoForge(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> builder, CallbackInfo ci) {
        ItemStackTooltipUtil.getTooltip((ItemStack) (Object) this, display, player, builder);
    }
}