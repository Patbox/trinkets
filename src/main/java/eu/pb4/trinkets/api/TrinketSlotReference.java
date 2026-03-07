package eu.pb4.trinkets.api;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public record TrinketSlotReference(SlotType slotType, int index) implements StringRepresentable {
    public String getSerializedName() {
        return this.slotType.getId() + "/" + index;
    }
}