package eu.pb4.trinkets.api;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public record TrinketSlotAccess(TrinketInventory inventory, int index) implements SlotAccess, StringRepresentable {
    public String getSerializedName() {
        return this.slotType().getId() + "@" + index;
    }

    public String getAsIdentifierPath() {
        return this.slotType().getId() + "/" + index;
    }

    public SlotType slotType() {
        return this.inventory.slotType();
    }

    @Override
    public ItemStack get() {
        return inventory.getItem(index);
    }

    @Override
    public boolean set(ItemStack itemStack) {
        inventory.setItem(index, itemStack);
        return true;
    }

    public TrinketSlotReference reference() {
        return new TrinketSlotReference(this.slotType(), index);
    }

    public boolean isValid() {
        return inventory.isValidSlot(index);
    }
}