package eu.pb4.trinkets.impl;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketSlot {

	public boolean isTrinketFocused();

	public boolean renderAfterRegularSlots();

	public SlotType getType();

	TrinketSlotAccess getAccess();

	public static boolean canInsert(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
		boolean res = slotRef.inventory().slotType().validatorCheck(stack, slotRef, entity);

		if (res) {
			return TrinketCallback.getCallback(stack).canEquip(stack, slotRef, entity);
		}

		return false;
	}

	static boolean mayPickup(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
		return TrinketCallback.getCallback(stack).canUnequip(stack, slotRef, entity);
	}
}
