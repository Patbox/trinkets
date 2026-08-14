package eu.pb4.trinkets.impl.client.compat;

import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsConfig;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketScreenManager;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import java.util.ArrayList;
import java.util.List;

import eu.pb4.trinkets.impl.client.slot.ClientTrinketSlotState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.Rect2i;

@Environment(EnvType.CLIENT)
public class TrinketsExclusionAreas {

	public static List<Rect2i> create(Screen screen) {
		if (screen instanceof TrinketScreen trinketScreen && trinketScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			return state.getScreenBackend().getExclusionAreas(trinketScreen);
		}
		return List.of();
	}
}
