package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public interface TrinketScreen {

	public TrinketPlayerScreenHandler trinkets$getHandler();

	public Rect2i trinkets$getGroupRect(SlotGroup group);

	public Slot trinkets$getFocusedSlot();
	
	public int trinkets$getX();
	
	public int trinkets$getY();

	public default boolean trinkets$isRecipeBookOpen() {
		return false;
	}

	public default void trinkets$updateTrinketSlots() {
	}
}
