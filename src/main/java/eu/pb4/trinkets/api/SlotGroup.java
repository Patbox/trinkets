package eu.pb4.trinkets.api;

import eu.pb4.trinkets.impl.SlotGroupImpl;
import eu.pb4.trinkets.impl.SlotTypeImpl;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.NonExtendable
public interface SlotGroup {
    @Deprecated(forRemoval = true)
    int slotId();
    boolean isAttachedToSlot(Slot slot);
    boolean hasSlotAttachment();
    int order();
    String name();
    Map<String, SlotType> slots();
}
