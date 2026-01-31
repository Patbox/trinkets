package eu.pb4.trinkets.mixin.client;

import eu.pb4.trinkets.impl.client.TrinketScreenManager;
import eu.pb4.trinkets.impl.TrinketSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookScreenMixin extends AbstractContainerScreen<RecipeBookMenu> {
    @Unique
    private static final Identifier SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    @Unique
    private static final Identifier SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    @Unique
    private static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot");

    public RecipeBookScreenMixin(RecipeBookMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }


    @Inject(at = @At("HEAD"), method = "hasClickedOutside", cancellable = true)
    private void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> info) {
        if (TrinketScreenManager.isClickInsideTrinketBounds(mouseX, mouseY)) {
            info.setReturnValue(false);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;renderCarriedItem(Lnet/minecraft/client/gui/GuiGraphics;II)V", shift = At.Shift.BEFORE),
            method = "render")
    private void drawForeground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (((Object) this) instanceof InventoryScreen) {
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
}
