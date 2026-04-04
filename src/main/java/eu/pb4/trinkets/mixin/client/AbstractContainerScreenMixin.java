package eu.pb4.trinkets.mixin.client;


import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.impl.client.CreativeTrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketScreenManager;
import org.jetbrains.annotations.Nullable;
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
					if (((CreativeSlotAccessor) cs).getSlot().index != TrinketsClient.activeGroup.slotId()) {
						info.setReturnValue(false);
					}
				} else if (slot.index != TrinketsClient.activeGroup.slotId()) {
					info.setReturnValue(false);
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
}
