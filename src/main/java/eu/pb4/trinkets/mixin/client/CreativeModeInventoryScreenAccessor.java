package eu.pb4.trinkets.mixin.client;

import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Accessor
    static CreativeModeTab getSelectedTab() {
        throw new UnsupportedOperationException();
    }
}
