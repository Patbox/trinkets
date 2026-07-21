package eu.pb4.trinkets.mixin.neo;

import eu.pb4.trinkets.api.ext.TrinketsItemStackExtension;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements TrinketsItemStackExtension {

}