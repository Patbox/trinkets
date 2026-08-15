package eu.pb4.trinkets.api;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public record TrinketSlotAccess(TrinketInventory inventory, int index, boolean cosmetic) implements SlotAccess, StringRepresentable {
    public TrinketSlotAccess(TrinketInventory inventory, int index) {
        this(inventory, index, false);
    }

    public String getSerializedName() {
        return this.slotType().getId() + "@" + index + (cosmetic ? "?cosmetic" : "");
    }

    public String getAsIdentifierPath() {
        return this.slotType().getId() + "/" + index + (cosmetic ? "/_/cosmetic" : "");
    }

    public SlotType slotType() {
        return this.inventory.slotType();
    }

    @Override
    public ItemStack get() {
        if (this.isValid()) {
            return this.cosmetic ? inventory.getCosmeticItem(index) : inventory.getItem(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean set(ItemStack itemStack) {
        if (this.isValid()) {
            if (this.cosmetic) {
                inventory.setCosmeticItem(index, itemStack);
            } else {
                inventory.setItem(index, itemStack);
            }
            return true;
        }
        return false;
    }

    public int maxStackSize(ItemStack stack) {
        return this.inventory.getMaxStackSize(stack);
    }

    public TrinketSlotReference reference() {
        return new TrinketSlotReference(this.slotType().getId(), index, cosmetic);
    }

    public boolean isValid() {
        return inventory.isValidSlot(index) && (!cosmetic || inventory.hasCosmeticItems());
    }

    public boolean canApplyEffects() {
        return !this.cosmetic && this.inventory.getAttachment().canApplyEffects(this.get(), this);
    }

    public boolean canApplyEffects(ItemStack otherStack) {
        return !this.cosmetic && this.inventory.getAttachment().canApplyEffects(otherStack, this);
    }

    public boolean isVisible() {
        return this.inventory.isVisible(this.index);
    }
}