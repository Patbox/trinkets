package eu.pb4.trinkets.api;

import net.minecraft.world.Container;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.NonExtendable
public interface TrinketInventory extends Container {
    SlotType slotType();

    @Nullable
    TrinketSlotAccess getSlotAccess(int slot);

    TrinketSlotAccess getOrCreateSlotAccess(int slot);

    boolean isValidSlot(int index);

    TrinketAttachment getAttachment();

    default boolean isVisible(int i) {
        return true;
    }

    @Deprecated(forRemoval = true)
    void copyFrom(TrinketInventory value);
}
