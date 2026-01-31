package eu.pb4.trinkets.impl;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotReference;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import eu.pb4.trinkets.mixin.client.accessor.RecipeBookScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A gui slot for a trinket slot, used in the survival inventory, but suited for any case
 */
public class SurvivalTrinketSlot extends Slot implements TrinketSlot {
	private final SlotGroup group;
	private final SlotType type;
	private final boolean alwaysVisible;
	private final int slotOffset;
	private final TrinketInventory trinketInventory;

	public SurvivalTrinketSlot(TrinketInventory inventory, int index, int x, int y, SlotGroup group, SlotType type, int slotOffset,
			boolean alwaysVisible) {
		super(inventory, index, x, y);
		this.group = group;
		this.type = type;
		this.slotOffset = slotOffset;
		this.alwaysVisible = alwaysVisible;
		this.trinketInventory = inventory;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return TrinketSlot.canInsert(stack, new SlotReference(trinketInventory, slotOffset), trinketInventory.getComponent().getEntity());
	}

	@Override
	public boolean mayPickup(Player player) {
		ItemStack stack = this.getItem();
		return TrinketsApi.getTrinket(stack.getItem())
			.canUnequip(stack, new SlotReference(trinketInventory, slotOffset), player);
	}

	@Override
	public boolean isActive() {
		if (alwaysVisible) {
			if (x < 0) {
				Level world = trinketInventory.getComponent().getEntity().level();
				if (world.isClientSide()) {
					Minecraft client = Minecraft.getInstance();
					Screen s = client.screen;
					if (s instanceof InventoryScreen screen) {
						if (((RecipeBookScreenAccessor) screen).getRecipeBookComponent().isVisible()) {
							return false;
						}
					}
				}
			}
			return true;
		}
		return isTrinketFocused();
	}

	@Override
	public boolean isTrinketFocused() {
		if (TrinketsClient.activeGroup == group) {
			return slotOffset == 0 || TrinketsClient.activeType == type;
		} else if (TrinketsClient.quickMoveGroup == group) {
			return slotOffset == 0 || TrinketsClient.quickMoveType == type && TrinketsClient.quickMoveTimer > 0;
		}
		return false;
	}

	@Override
	public boolean renderAfterRegularSlots() {
		return slotOffset != 0 || !this.alwaysVisible;
	}

	@Override
	public @Nullable Identifier getNoItemIcon() {
		return type.getIcon();
	}

	@Override
	public SlotType getType() {
		return type;
	}
}
