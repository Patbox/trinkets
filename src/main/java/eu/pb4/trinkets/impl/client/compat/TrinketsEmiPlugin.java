package eu.pb4.trinkets.impl.client.compat;
/*
import dev.emi.api.EmiPlugin;
import dev.emi.api.EmiRegistry;
import dev.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;

public class TrinketsEmiPlugin implements EmiPlugin {

	@Override
	public void register(EmiRegistry registry) {
		registry.addExclusionArea(InventoryScreen.class, (screen, consumer) -> {
			for (Rect2i rect2i : TrinketsExclusionAreas.create(screen)) {
				consumer.accept(new Bounds(rect2i.getX(), rect2i.getY(), rect2i.getWidth(),
					rect2i.getHeight()));
			}
		});
		registry.addExclusionArea(CreativeModeInventoryScreen.class, (screen, consumer) -> {
			for (Rect2i rect2i : TrinketsExclusionAreas.create(screen)) {
				consumer.accept(new Bounds(rect2i.getX(), rect2i.getY(), rect2i.getWidth(),
					rect2i.getHeight()));
			}
		});
	}
}
*/