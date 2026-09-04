package eu.pb4.trinkets.impl.platform.neo.compat;

import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.client.compat.TrinketsExclusionAreas;
import eu.pb4.trinkets.impl.client.compat.TrinketsREIPlugin;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.stream.Collectors;

@REIPluginClient
public class TrinketsNeoREIPlugin implements REIClientPlugin {
	@Override
	public void registerExclusionZones(ExclusionZones zones) {
		new TrinketsREIPlugin().registerExclusionZones(zones);
	}
}