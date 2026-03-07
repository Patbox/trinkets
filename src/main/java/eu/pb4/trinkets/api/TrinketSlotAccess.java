package eu.pb4.trinkets.api;

import eu.pb4.trinkets.impl.TrinketInventoryImpl;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public record TrinketSlotAccess(TrinketInventory inventory, int index) implements SlotAccess, StringRepresentable {
    public String getSerializedName() {
        return this.inventory.getSlotType().getId() + "/" + index;
    }

    public SlotType slotType() {
        return this.inventory.getSlotType();
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
}