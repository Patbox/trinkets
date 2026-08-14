package eu.pb4.trinkets.impl.slots;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.impl.TrinketInventoryImpl;
import eu.pb4.trinkets.impl.TrinketUtilities;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import eu.pb4.trinkets.mixin.client.accessor.RecipeBookScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A gui slot for a trinket slot, used in the survival inventory, but suited for any case
 */
public class SurvivalTrinketSlot extends Slot implements TrinketSlot {
	private final SlotGroup group;
	private final SlotType type;
	private final TrinketSlotAccess ref;
	private final LivingEntity owner;
	private final int slot;
	private final Predicate<TrinketSlot> visibilityPredicate;
	private final boolean renderAfterRegularSlots;

	public SurvivalTrinketSlot(TrinketInventoryImpl inventory, int slot, int x, int y,
							   Predicate<TrinketSlot> visibilityPredicate,
							   boolean renderAfterRegularSlots,
							   LivingEntity owner) {
		super(inventory, slot, x, y);
		this.group = SlotGroup.getEntityGroups(owner).get(inventory.slotType().group());
		this.type = inventory.slotType();
		this.slot = slot;
		this.visibilityPredicate = visibilityPredicate;
		this.renderAfterRegularSlots = renderAfterRegularSlots;
		this.ref = inventory.getSlotAccess(this.slot);
		this.owner = owner;
	}

	@Override
	public void setByPlayer(ItemStack itemStack, ItemStack previous) {
		super.setByPlayer(itemStack, previous);
		TrinketUtilities.playEquipmentSound(itemStack, ref, owner);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return TrinketSlot.canInsert(stack, this.ref, this.owner);
	}

	@Override
	public boolean mayPickup(Player player) {
		return TrinketSlot.mayPickup(this.getItem(), this.ref, player);
	}

	@Override
	public boolean isActive() {
		if (!this.ref.isValid()) {
			return false;
		}

		if (!this.renderAfterRegularSlots && this.visibilityPredicate.test(this)) {
			if (x < 0) {
				Level world = this.owner.level();
				if (TrinketsMain.IS_CLIENT && world.isClientSide()) {
					Minecraft client = Minecraft.getInstance();
					Screen s = client.gui.screen();
					if (s instanceof InventoryScreen screen) {
						if (((RecipeBookScreenAccessor) screen).trinkets$getRecipeBookComponent().isVisible()) {
							return false;
						}
					}
				}
			}
			return true;
		}

		return this.visibilityPredicate.test(this);
	}

	@Override
	public boolean isTrinketFocused() {
		return this.visibilityPredicate.test(this);
	}

	@Override
	public boolean renderAfterRegularSlots() {
		return this.renderAfterRegularSlots;
	}

	@Override
	public @Nullable Identifier getNoItemIcon() {
		return type.icon();
	}

	@Override
	public SlotType getType() {
		return type;
	}

	@Override
	public TrinketSlotAccess getAccess() {
		return this.ref;
	}

	@Override
	public SlotGroup getGroup() {
		return this.group;
	}
}
