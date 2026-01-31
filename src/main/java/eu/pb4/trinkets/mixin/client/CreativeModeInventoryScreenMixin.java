package eu.pb4.trinkets.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import eu.pb4.trinkets.impl.client.CreativeTrinketScreen;
import eu.pb4.trinkets.impl.client.CreativeTrinketSlot;
import eu.pb4.trinkets.impl.Point;
import eu.pb4.trinkets.impl.SurvivalTrinketSlot;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketScreenManager;
import eu.pb4.trinkets.impl.TrinketSlot;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Delegates drawing and slot group selection logic
 * 
 * @author Emi
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<ItemPickerMenu> implements TrinketScreen, CreativeTrinketScreen {
	@Unique
	private static final Identifier SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.withDefaultNamespace("container/slot_highlight_front");
	@Unique
	private static final Identifier SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.withDefaultNamespace("container/slot_highlight_back");
	@Unique
	private static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot");
	@Shadow
	private static CreativeModeTab selectedTab;
	@Shadow
	protected abstract void selectTab(CreativeModeTab group);

	private CreativeModeInventoryScreenMixin() {
		super(null, null, null);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"), method = "selectTab")
	private int size(NonNullList<ItemStack> list) {
		return 46;
	}

	@Inject(at = @At("HEAD"), method = "selectTab")
	private void setSelectedTab(CreativeModeTab g, CallbackInfo info) {
		if (g.getType() != CreativeModeTab.Type.INVENTORY) {
			TrinketScreenManager.removeSelections();
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;<init>(Lnet/minecraft/world/Container;III)V"), method = "selectTab")
	private void addCreativeTrinketSlots(CreativeModeTab g, CallbackInfo info) {
		TrinketPlayerScreenHandler handler = trinkets$getHandler();
		for (int i = handler.trinkets$getTrinketSlotStart(); i < handler.trinkets$getTrinketSlotEnd(); i++) {
			Slot slot = this.minecraft.player.inventoryMenu.slots.get(i);
			if (slot instanceof SurvivalTrinketSlot ts) {
				SlotGroup group = TrinketsApi.getPlayerSlots(this.minecraft.player).get(ts.getType().getGroup());
				Rect2i rect = trinkets$getGroupRect(group);
				Point pos = trinkets$getHandler().trinkets$getGroupPos(group);
				if (pos == null) {
					return;
				}
				int xOff = rect.getX() + 1 - pos.x();
				int yOff = rect.getY() + 1 - pos.y();
				((ItemPickerMenu) this.menu).slots.add(new CreativeTrinketSlot(ts, ts.getContainerSlot(), ts.x + xOff, ts.y + yOff));
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "init")
	private void init(CallbackInfo info) {
		TrinketScreenManager.init(this);
	}

	@Inject(at = @At("HEAD"), method = "removed")
	private void removed(CallbackInfo info) {
		TrinketScreenManager.removeSelections();
	}

	@Inject(at = @At("TAIL"), method = "containerTick")
	private void tick(CallbackInfo info) {
		TrinketScreenManager.tick();
	}

	@Inject(at = @At("HEAD"), method = "render")
	private void render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
			TrinketScreenManager.update(mouseX, mouseY);
		}
	}

	@Inject(at = @At("RETURN"), method = "renderBg")
	private void drawBackground(GuiGraphics context, float delta, int mouseX, int mouseY, CallbackInfo info) {
		if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
			TrinketScreenManager.drawBackground(context);
		}
	}

	@Override
	public void trinkets$renderCreative(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
		if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
			context.pose().pushMatrix();
			context.pose().translate(this.leftPos, this.topPos);
			TrinketScreenManager.drawForeground(context);

			for (Slot slot : this.menu.slots) {
				if (slot instanceof TrinketSlot trinketSlot && trinketSlot.renderAfterRegularSlots() && slot.isActive()) {
					context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, slot.x - 1, slot.y - 1, 18, 18);

					if (slot == this.hoveredSlot && slot.isHighlightable()) {
						context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_TEXTURE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
					}
					this.renderSlot(context, slot, mouseX, mouseY);
					if (slot == this.hoveredSlot && slot.isHighlightable()) {
						context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_TEXTURE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
					}
				}
			}
			context.pose().popMatrix();
		}
	}

	@Inject(at = @At("HEAD"), method = "hasClickedOutside", cancellable = true)
	private void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> info) {
		if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY && TrinketScreenManager.isClickInsideTrinketBounds(mouseX, mouseY)) {
			info.setReturnValue(false);
		}
	}

	@Inject(at = @At("HEAD"), method = "checkTabClicked", cancellable = true)
	private void isClickInTab(CreativeModeTab group, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			info.setReturnValue(false);
		}
	}
	
	@Inject(at = @At("HEAD"), method = "checkTabHovering", cancellable = true)
	private void renderTabTooltipIfHovered(GuiGraphics context, CreativeModeTab group, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			info.setReturnValue(false);
		}
	}

	@Override
	public TrinketPlayerScreenHandler trinkets$getHandler() {
		return (TrinketPlayerScreenHandler) this.minecraft.player.inventoryMenu;
	}

	@Override
	public Rect2i trinkets$getGroupRect(SlotGroup group) {
		int groupNum = trinkets$getHandler().trinkets$getGroupNum(group);
		if (groupNum <= 3) {
			// Look what else do you want me to do
			return switch (groupNum) {
				case 1 -> new Rect2i(15, 19, 17, 17);
				case 2 -> new Rect2i(126, 19, 17, 17);
				case 3 -> new Rect2i(145, 19, 17, 17);
				case -5 -> new Rect2i(53, 5, 17, 17);
				case -6 -> new Rect2i(53, 32, 17, 17);
				case -7 -> new Rect2i(107, 5, 17, 17);
				case -8 -> new Rect2i(107, 32, 17, 17);
				case -45 -> new Rect2i(34, 19, 17, 17);
				default -> new Rect2i(0, 0, 0, 0);
			};
		}
		Point pos = trinkets$getHandler().trinkets$getGroupPos(group);
		if (pos != null) {
			return new Rect2i(pos.x() - 1, pos.y() - 1, 17, 17);
		}
		return new Rect2i(0, 0, 0, 0);
	}

	@Override
	public Slot trinkets$getFocusedSlot() {
		return this.hoveredSlot;
	}

	@Override
	public int trinkets$getX() {
		return this.leftPos;
	}

	@Override
	public int trinkets$getY() {
		return this.topPos;
	}

	@Override
	public boolean trinkets$isRecipeBookOpen() {
		return false;
	}

	@Override
	public void trinkets$updateTrinketSlots() {
		selectTab(selectedTab);
	}
}
