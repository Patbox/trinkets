package eu.pb4.trinkets.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public interface ItemPickerMenuAccessor {
    @Accessor
    AbstractContainerMenu getInventoryMenu();
}
