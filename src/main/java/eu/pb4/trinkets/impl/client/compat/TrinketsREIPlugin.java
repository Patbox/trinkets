package eu.pb4.trinkets.impl.client.compat;
/*
import eu.pb4.trinkets.impl.TrinketScreen;
import java.util.List;
import java.util.stream.Collectors;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.minecraft.client.gui.screens.Screen;

public class TrinketsREIPlugin implements REIClientPlugin {

	@Override
	public void registerExclusionZones(ExclusionZones zones) {
		zones.register(TrinketScreen.class, screen -> {
			if (screen instanceof Screen guiScreen) {
				return TrinketsExclusionAreas.create(guiScreen).stream().map(
					rect2i -> new Rectangle(rect2i.getX(), rect2i.getY(), rect2i.getWidth(),
						rect2i.getHeight())).collect(Collectors.toList());
			}
			return List.of();
		});
	}
}
*/