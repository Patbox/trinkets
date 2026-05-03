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
        if (this.isValid()) {
            return inventory.getItem(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean set(ItemStack itemStack) {
        if (this.isValid()) {
            inventory.setItem(index, itemStack);
            return true;
        }
        return false;
    }

    public int maxStackSize(ItemStack stack) {
        return this.inventory.getMaxStackSize(stack);
    }

    public TrinketSlotReference reference() {
        return new TrinketSlotReference(this.slotType(), index);
    }

    public boolean isValid() {
        return inventory.isValidSlot(index);
    }

    public boolean canApplyEffects() {
        return this.inventory.getAttachment().canApplyEffects(this.get(), this);
    }

    public boolean canApplyEffects(ItemStack otherStack) {
        return this.inventory.getAttachment().canApplyEffects(otherStack, this);
    }

    public boolean isVisible() {
        return this.inventory.isVisible(this.index);
    }
}