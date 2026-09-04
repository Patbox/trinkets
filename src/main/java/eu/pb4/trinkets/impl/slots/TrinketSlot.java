package eu.pb4.trinkets.impl.slots;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.event.TrinketCanEquipCallback;
import eu.pb4.trinkets.api.event.TrinketCanUnequipCallback;
import eu.pb4.trinkets.api.event.TrinketSlotCompatibilityCallback;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface TrinketSlot {

    static boolean canInsert(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
        var res = isSlotCompatible(stack, slotRef, entity);

        if (res) {
            return isEquipable(stack, slotRef, entity);
        }

        return false;
    }

    static boolean isSlotCompatible(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
        boolean res = slotRef.inventory().slotType().validatorCheck(stack, slotRef, entity);
        return TrinketSlotCompatibilityCallback.EVENT.invoker().isTrinketSlotCompatible(stack, slotRef, entity, res).toBooleanOrElse(res);
    }

    static boolean isEquipable(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
        boolean res = TrinketCallback.getCallback(stack).canEquip(stack, slotRef, entity);
        return TrinketCanEquipCallback.EVENT.invoker().canEquip(stack, slotRef, entity, res).toBooleanOrElse(res);
    }

    static boolean mayPickup(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
        var res = TrinketCallback.getCallback(stack).canUnequip(stack, slotRef, entity);
        return TrinketCanUnequipCallback.EVENT.invoker().canUnequip(stack, slotRef, entity, res).toBooleanOrElse(res);
    }

    boolean isTrinketFocused();

    boolean renderAfterRegularSlots();

    SlotType getType();

    TrinketSlotAccess getAccess();

    SlotGroup getGroup();

    default Slot asSlot() {
        return (Slot) this;
    }

    LivingEntity getOwner();

    boolean isDecorativeMode();

    ItemStack getTrinketGhostItem();
}
