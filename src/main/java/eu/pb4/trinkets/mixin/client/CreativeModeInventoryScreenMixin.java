package eu.pb4.trinkets.mixin.client;

import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.impl.TrinketsConfig;
import eu.pb4.trinkets.impl.slots.TrinketSlotState;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.item.CreativeModeTabs;
import org.jspecify.annotations.Nullable;
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
import eu.pb4.trinkets.impl.slots.SurvivalTrinketSlot;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketScreenManager;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
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
	@Unique
	@Nullable
    private AbstractWidget decorativeModeButton;

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
			if (this.decorativeModeButton != null) {
				this.removeWidget(this.decorativeModeButton);
			}
		} else if (this.decorativeModeButton != null) {
			this.addRenderableWidget(this.decorativeModeButton);
		}

	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;<init>(Lnet/minecraft/world/Container;III)V"), method = "selectTab")
	private void addCreativeTrinketSlots(CreativeModeTab g, CallbackInfo info) {
		TrinketInventoryMenu handler = trinkets$getHandler();
		for (int i = handler.trinkets$getTrinketSlotStart(); i < handler.trinkets$getTrinketSlotEnd(); i++) {
			Slot slot = this.minecraft.player.inventoryMenu.slots.get(i);
			if (slot instanceof SurvivalTrinketSlot ts) {
				var slotInfo = trinkets$getSlotState().getSlotConfig(i - handler.trinkets$getTrinketSlotStart(),
						(TrinketInventory) ts.container, ts.getContainerSlot());
				if (slotInfo == null) {
					continue;
				}
				((ItemPickerMenu) this.menu).slots.add(new CreativeTrinketSlot(ts, ts.getContainerSlot(), slotInfo.x(), slotInfo.y()));
			}
		}
    }

	@Inject(at = @At("HEAD"), method = "init")
	private void init(CallbackInfo info) {
		TrinketScreenManager.init(this);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setVisible(Z)V"), method = "init")
	private void addButton(CallbackInfo info) {
		if (TrinketsConfig.serverSyncedGameplay.cosmeticSlots) {
			this.decorativeModeButton = TrinketScreenManager.createToggleDecorativeModeButton(this, () -> this.leftPos, this.topPos);
		}
	}

	@Inject(at = @At("HEAD"), method = "removed")
	private void removed(CallbackInfo info) {
		TrinketScreenManager.removeSelections();
	}

	@Inject(at = @At("TAIL"), method = "containerTick")
	private void tick(CallbackInfo info) {
		TrinketScreenManager.tick();
	}

	@Inject(at = @At("HEAD"), method = "extractRenderState")
	private void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
			TrinketScreenManager.update(mouseX, mouseY);
		}
	}

	@Inject(at = @At("RETURN"), method = "extractBackground")
	private void drawBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
			TrinketScreenManager.drawBackground(graphics);
		}
	}

	@Override
	public void trinkets$renderCreative(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
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
					this.extractSlot(context, slot, mouseX, mouseY);
					if (slot == this.hoveredSlot && slot.isHighlightable()) {
						context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_TEXTURE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
					}

					TrinketScreenManager.drawSlotExtrasLateDraw(slot, trinketSlot, context);
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
	private void renderTabTooltipIfHovered(GuiGraphicsExtractor context, CreativeModeTab group, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			info.setReturnValue(false);
		}
	}

	@Override
	public TrinketInventoryMenu trinkets$getHandler() {
		return (TrinketInventoryMenu) this.minecraft.player.inventoryMenu;
	}


	@Override
	public TrinketSlotState trinkets$getSlotState() {
		return trinkets$getHandler().trinkets$getSlotState().asCreativeState();
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
		if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY) {
			return;
		}

		var old = selectedTab;
		selectTab(CreativeModeTabs.getDefaultTab());
		selectTab(old);
	}
}
