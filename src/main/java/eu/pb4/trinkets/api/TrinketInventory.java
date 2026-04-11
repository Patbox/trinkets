package eu.pb4.trinkets.api;

import net.minecraft.world.Container;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.NonExtendable
public interface TrinketInventory extends Container {
    void copyFrom(TrinketInventory value);
    SlotType slotType();

    @Nullable
    TrinketSlotAccess getSlotAccess(int slot);

    boolean isValidSlot(int index);

    TrinketAttachment getAttachment();
}
