package eu.pb4.trinkets.impl.client.slot.legacy;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.impl.Point;
import eu.pb4.trinkets.impl.slots.TrinketSlotState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface LegacyTrinketSlotState extends TrinketSlotState {
    int getGroupNum(SlotGroup group);

    @Nullable Point getGroupPos(SlotGroup group);

    @Nullable SlotGroup getGroupAtSlot(int slotIndex);

    @NotNull List<Point> getSlotHeights(SlotGroup group);

    @Nullable Point getSlotHeight(SlotGroup group, int i);

    @NotNull List<SlotType> getSlotTypes(SlotGroup group);

    int getSlotWidth(SlotGroup group);

    int groupCount();

    LegacyTrinketSlotState asCreativeState();

    default Area2i getGroupRect(SlotGroup group) {
        var x = getGroupPos(group);
        if (x != null) {
            return new Area2i(x.x() - 1, x.y() - 1, 18, 18);
        }

        return new Area2i(0, 0,0, 0);
    }

    boolean forceSidebar();
}
