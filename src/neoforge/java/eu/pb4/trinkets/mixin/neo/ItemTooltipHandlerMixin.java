package eu.pb4.trinkets.mixin.neo;

import eu.pb4.trinkets.api.ext.TrinketsItemStackExtension;
import eu.pb4.trinkets.impl.ItemStackTooltipUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.common.tooltip.ItemTooltipHandler;
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
@Mixin(ItemTooltipHandler.class)
public abstract class ItemTooltipHandlerMixin implements TrinketsItemStackExtension {
    @SuppressWarnings("MixinAnnotationTarget")
    @Inject(method = "addDetailsToTooltipMiddle", at = @At("TAIL"), require = 0)
    private static void getTooltipNeoForge(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> builder, CallbackInfo ci) {
        ItemStackTooltipUtil.getTooltip(stack, display, player, builder);
    }
}