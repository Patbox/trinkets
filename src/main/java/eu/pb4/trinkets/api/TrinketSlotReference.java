package eu.pb4.trinkets.api;

import net.minecraft.util.StringRepresentable;

public record TrinketSlotReference(SlotType slotType, int index) implements StringRepresentable {
    public String getSerializedName() {
        return this.slotType.getId() + "/" + index;
    }
}