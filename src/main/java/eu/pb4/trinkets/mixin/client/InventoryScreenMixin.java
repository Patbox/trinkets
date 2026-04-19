package eu.pb4.trinkets.mixin.client;

import eu.pb4.trinkets.mixin.client.accessor.RecipeBookScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import eu.pb4.trinkets.impl.Point;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketScreenManager;
import eu.pb4.trinkets.api.SlotGroup;

/**
 * Delegates drawing and slot group selection logic
 * 
 * @author Emi
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> implements RecipeUpdateListener, TrinketScreen {
	private InventoryScreenMixin() {
		super(null, null, null, null);
	}

	@Inject(at = @At("HEAD"), method = "init")
	private void init(CallbackInfo info) {
		TrinketScreenManager.init(this);
	}

	@Inject(at = @At("TAIL"), method = "containerTick")
	private void tick(CallbackInfo info) {
		TrinketScreenManager.tick();
	}

	@Inject(at = @At("HEAD"), method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
	private void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		TrinketScreenManager.update(mouseX, mouseY);
	}

	@Inject(at = @At("RETURN"), method = "extractBackground")
	private void drawBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		TrinketScreenManager.drawBackground(graphics);
	}

	@Override
	public TrinketInventoryMenu trinkets$getHandler() {
		return (TrinketInventoryMenu) this.menu;
	}
	
	@Override
	public Rect2i trinkets$getGroupRect(SlotGroup group) {
		Point pos = ((TrinketInventoryMenu) menu).trinkets$getGroupPos(group);
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
		return ((RecipeBookScreenAccessor) this).getRecipeBookComponent().isVisible();
	}

	@Override
	public boolean trinkets$isNarrow() {
		return ((RecipeBookScreenAccessor) this).getWidthTooNarrow();
	}
}
