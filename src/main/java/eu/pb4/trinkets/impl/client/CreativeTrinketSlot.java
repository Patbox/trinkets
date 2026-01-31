package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.impl.SurvivalTrinketSlot;
import eu.pb4.trinkets.impl.TrinketSlot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.SlotWrapper;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * A gui slot for a trinket slot in the creative inventory
 */
@Environment(EnvType.CLIENT)
public class CreativeTrinketSlot extends SlotWrapper implements TrinketSlot {
	private final SurvivalTrinketSlot original;

	public CreativeTrinketSlot(SurvivalTrinketSlot original, int s, int x, int y) {
		super(original, s, x, y);
		this.original = original;
	}

	@Override
	public boolean isTrinketFocused() {
		return original.isTrinketFocused();
	}

	@Override
	public boolean renderAfterRegularSlots() {
		return original.renderAfterRegularSlots();
	}

	@Override
	public SlotType getType() {
		return original.getType();
	}

	@Override
	public @Nullable Identifier getNoItemIcon() {
		return original.getNoItemIcon();
	}
}
