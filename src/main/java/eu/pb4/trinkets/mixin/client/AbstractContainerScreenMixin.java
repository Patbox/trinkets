package eu.pb4.trinkets.mixin.client;


import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.client.CreativeTrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketScreenManager;
import eu.pb4.trinkets.impl.TrinketsConfig;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import eu.pb4.trinkets.impl.TrinketSlot;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import eu.pb4.trinkets.mixin.client.accessor.CreativeSlotAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.SlotWrapper;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

/**
 * Draws trinket slot backs, adjusts z location of draw calls, and makes non-trinket slots un-interactable while a trinket slot group is focused
 * 
 * @author Emi
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
	@Shadow @Nullable protected Slot hoveredSlot;
	private AbstractContainerScreenMixin() {
		super(null);
	}

	@Shadow protected abstract void onStopHovering(Slot slot);

	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	@Shadow
	protected abstract boolean isHovering(Slot slot, double xm, double ym);

	@Inject(at = @At("HEAD"), method = "removed")
	private void removed(CallbackInfo info) {
		if ((Object)this instanceof InventoryScreen) {
			TrinketScreenManager.removeSelections();
		}
	}

	@WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V"),
			method = "extractSlots")
	private boolean preventDrawingSlots(AbstractContainerScreen instance, GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY) {
		return !(slot instanceof TrinketSlot trinketSlot) || !trinketSlot.renderAfterRegularSlots();
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.AFTER), method = "extractRenderState")
	private void renderCreativeSlots(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		if (this instanceof CreativeTrinketScreen screen) {
			screen.trinkets$renderCreative(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Inject(at = @At("HEAD"), method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", cancellable = true)
	private void isPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			if (slot instanceof TrinketSlot ts) {
				if (!ts.isTrinketFocused()) {
					info.setReturnValue(false);
				}
			} else {
				if (slot instanceof SlotWrapper cs) {
					if (!TrinketsClient.activeGroup.isAttachedToSlot(((CreativeSlotAccessor) cs).getSlot())) {
						info.setReturnValue(false);
					}
				} else if (!TrinketsClient.activeGroup.isAttachedToSlot(slot)) {
					info.setReturnValue(false);
				}
			}
		}
	}

	@Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true)
	private void preferLateTrinketsSlots(double x, double y, CallbackInfoReturnable<Slot> cir) {
		if (this instanceof TrinketScreen) {
			for (Slot slot : this.menu.slots) {
				if (slot instanceof TrinketSlot trinketSlot && trinketSlot.renderAfterRegularSlots() && slot.isActive() && this.isHovering(slot, x, y)) {
					cir.setReturnValue(slot);
					return;
				}
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "onStopHovering", cancellable = true)
	private void onStopHovering(Slot slot, CallbackInfo info) {
		if (slot instanceof TrinketSlot && slot.container instanceof TrinketInventory inventory) {
			if (slot.index >= inventory.getContainerSize()) {
				if (slot != this.hoveredSlot && this.hoveredSlot != null) {
					this.onStopHovering(this.hoveredSlot);
				}
				info.cancel();
			}
		}
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"), method = "mouseClicked")
	private boolean overrideRecipeBookClick(AbstractContainerScreen<?> instance, MouseButtonEvent event, final boolean doubleClick, Operation<Boolean> original) {
		if (TrinketScreenManager.isClickInsideTrinketBounds(event.x(), event.y()) && this.hoveredSlot != null) {
			Optional<GuiEventListener> hoveredElement = this.getChildAt(event.x(), event.y());
			if(hoveredElement.isPresent() && hoveredElement.get() instanceof ImageButton) {
				return false;
			}
		}
		return original.call(instance, event, doubleClick);
	}

	@Inject(method = "extractSlotHighlightFront", at = @At("TAIL"))
	private void drawMoreSlotsIndicator(GuiGraphicsExtractor context, CallbackInfo ci) {
        //noinspection ConstantValue
        if (((Object) this) instanceof CreativeModeInventoryScreen s && CreativeModeInventoryScreenAccessor.getSelectedTab().getType() != CreativeModeTab.Type.INVENTORY) {
			return;
		}

		if ((this.menu instanceof ItemPickerMenuAccessor accessor ? accessor.getInventoryMenu() : this.menu) instanceof TrinketInventoryMenu trinketMenu
				&& TrinketsConfig.instance.showSlotsIndicator) {
			for (int i = 0; i < this.menu.slots.size(); i++) {
				Slot slot = this.menu.slots.get(i);
				if (slot instanceof TrinketSlot trinketSlot) {
					var g = trinketMenu.trinkets$getGroupAtSlot(i);
					if (!trinketSlot.renderAfterRegularSlots() && slot.isActive() && trinketSlot.getAccess().index() == 0 && TrinketsClient.activeGroup != g && g != null) {
						context.blitSprite(RenderPipelines.GUI_TEXTURED, TrinketScreenManager.MORE_SLOTS_INDICATOR_HORIZONTAL, slot.x - 8, slot.y - 8, 32, 32);
					}
					if (!trinketSlot.renderAfterRegularSlots() && slot.isActive() && trinketSlot.getAccess().inventory().getContainerSize() > 1 && trinketSlot.getAccess().index() == 0 && TrinketsClient.activeType != trinketSlot.getType()) {
						context.blitSprite(RenderPipelines.GUI_TEXTURED, TrinketScreenManager.MORE_SLOTS_INDICATOR_VERTICAL_STANDALONE, slot.x - 8, slot.y - 8, 32, 32);
					}
				} else {
					var g = trinketMenu.trinkets$getGroupAtSlot(i);
					if (g != null && TrinketScreenManager.group != g) {
						context.blitSprite(RenderPipelines.GUI_TEXTURED, TrinketScreenManager.MORE_SLOTS_INDICATOR_HORIZONTAL, slot.x - 8, slot.y - 8, 32, 32);
					}
				}
			}
		}
	}
}
