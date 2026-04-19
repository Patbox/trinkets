package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public interface TrinketScreen {

    TrinketInventoryMenu trinkets$getHandler();

    Rect2i trinkets$getGroupRect(SlotGroup group);

    Slot trinkets$getFocusedSlot();

    int trinkets$getX();

    int trinkets$getY();

    default boolean trinkets$isRecipeBookOpen() {
        return false;
    }

    default boolean trinkets$isNarrow() {
        return false;
    }

    default void trinkets$updateTrinketSlots() {
    }
}
