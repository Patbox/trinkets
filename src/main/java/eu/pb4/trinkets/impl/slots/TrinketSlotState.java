package eu.pb4.trinkets.impl.slots;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.impl.Point;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface TrinketSlotState {
    void createSlots(Consumer<Slot> slotConsumer);

    int getGroupNum(SlotGroup group);

    @Nullable Point getGroupPos(SlotGroup group);

    @Nullable SlotGroup getGroupAtSlot(int slotIndex);

    @NotNull List<Point> getSlotHeights(SlotGroup group);

    @Nullable Point getSlotHeight(SlotGroup group, int i);

    @NotNull List<SlotType> getSlotTypes(SlotGroup group);

    int getSlotWidth(SlotGroup group);

    int groupCount();

    TrinketSlotState asCreativeState();

    default Area2i getGroupRect(SlotGroup group) {
        var x = getGroupPos(group);
        if (x != null) {
            return new Area2i(x.x() - 1, x.y() - 1, 18, 18);
        }

        return new Area2i(0, 0,0, 0);
    }
    
    public record Area2i(int x, int y, int width, int height) {
        public boolean contains(int x, int y) {
            return x >= this.x && y >= this.y && x < this.x + width && y < this.y + height;
        }
    };
}
