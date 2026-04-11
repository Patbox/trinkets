package eu.pb4.trinkets.api;

import net.minecraft.util.StringRepresentable;

public record TrinketSlotReference(String slot, int index) implements StringRepresentable {
    public TrinketSlotReference(SlotType slotType, int index) {
        this(slotType.getId(), index);
    }

    public String getSerializedName() {
        return this.slot + "@" + index;
    }

    public String getAsIdentifierPath() {
        return this.slot + "/" + index;
    }
}