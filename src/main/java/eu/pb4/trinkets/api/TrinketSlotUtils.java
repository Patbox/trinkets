package eu.pb4.trinkets.api;

import eu.pb4.trinkets.impl.slots.StandaloneTrinketSlot;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class TrinketSlotUtils {
    private TrinketSlotUtils() {
    }

    public static Slot createSlot(TrinketSlotAccess access, int x, int y) {
        return new StandaloneTrinketSlot(access, x, y);
    }

    public static boolean mayPlace(TrinketSlotAccess access, ItemStack stack) {
        return TrinketSlot.canInsert(stack, access, access.inventory().getAttachment().getEntity());
    }

    public static boolean mayPickup(TrinketSlotAccess access, ItemStack stack) {
        return TrinketSlot.mayPickup(stack, access, access.inventory().getAttachment().getEntity());
    }

    public static boolean isSlotCompatible(TrinketSlotAccess access, ItemStack stack) {
        return TrinketSlot.isSlotCompatible(stack, access, access.inventory().getAttachment().getEntity());
    }

    public static boolean isEquipable(TrinketSlotAccess access, ItemStack stack) {
        return TrinketSlot.isEquipable(stack, access, access.inventory().getAttachment().getEntity());
    }

}
