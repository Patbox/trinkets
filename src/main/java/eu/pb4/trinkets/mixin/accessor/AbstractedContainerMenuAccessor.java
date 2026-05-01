package eu.pb4.trinkets.mixin.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface AbstractedContainerMenuAccessor {

    @Accessor("lastSlots")
    NonNullList<ItemStack> trinkets$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<RemoteSlot> trinkets$getRemoteSlots();
}
