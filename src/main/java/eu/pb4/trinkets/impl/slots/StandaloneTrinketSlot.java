package eu.pb4.trinkets.impl.slots;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.TrinketInventoryImpl;
import eu.pb4.trinkets.impl.TrinketUtilities;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A gui slot for a trinket slot, used in the survival inventory, but suited for any case
 */
public class StandaloneTrinketSlot extends Slot {
    private final TrinketSlotAccess access;

    public StandaloneTrinketSlot(TrinketSlotAccess access, int x, int y) {
        super(access.inventory(), access.index(), x, y);
        this.access = access;
    }

    @Override
    public void setByPlayer(ItemStack itemStack, ItemStack previous) {
        super.setByPlayer(itemStack, previous);
        TrinketUtilities.playEquipmentSound(itemStack, access, access.inventory().getAttachment().getEntity());
    }

    @Override
    public ItemStack getItem() {
        return this.access.get();
    }

    @Override
    public void set(ItemStack itemStack) {
        this.access.set(itemStack);
        this.setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.access.cosmetic()) {
            return ((TrinketInventoryImpl) this.access.inventory()).removeCosmeticItem(this.access.index(), amount);
        }

        return this.access.inventory().removeItem(this.access.index(), amount);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return TrinketSlot.canInsert(stack, this.access, this.access.inventory().getAttachment().getEntity());
    }

    @Override
    public boolean mayPickup(Player player) {
        return TrinketSlot.mayPickup(this.getItem(), this.access, player);
    }

    @Override
    public boolean isActive() {
        return this.access.isValid();
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return this.access.slotType().icon();
    }
}
