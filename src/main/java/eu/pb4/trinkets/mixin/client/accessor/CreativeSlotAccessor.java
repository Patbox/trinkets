package eu.pb4.trinkets.mixin.client.accessor;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.SlotWrapper;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * You'll access widen this into being accessible but won't make its field accessible? Yes.
 */
@Mixin(SlotWrapper.class)
public interface CreativeSlotAccessor {
	
	@Accessor("target")
	public Slot getSlot();
}
