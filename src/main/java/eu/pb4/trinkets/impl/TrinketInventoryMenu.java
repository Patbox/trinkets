package eu.pb4.trinkets.impl;

import java.util.List;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.impl.slots.TrinketSlotState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface for putting methods onto the player's screen handler
 */
public interface TrinketInventoryMenu {

	/**
	 * Called to inform the player's slot handler that it needs to remove and re-add its trinket slots to reflect new changes
	 */
	void trinkets$updateTrinketSlots(boolean reinitializeAttachment);

	int trinkets$getTrinketSlotStart();

	int trinkets$getTrinketSlotEnd();

	TrinketSlotState trinkets$getSlotState();
}
