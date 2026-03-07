package eu.pb4.trinkets.api;

import net.minecraft.world.Container;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface TrinketInventory extends Container {
    void copyFrom(TrinketInventory value);
    SlotType getSlotType();
}
