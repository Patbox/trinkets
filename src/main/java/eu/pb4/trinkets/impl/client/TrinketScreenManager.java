package eu.pb4.trinkets.impl.client;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.IntSupplier;

import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.impl.TrinketInventoryImpl;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsConfig;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.client.render.ClientTrinketsManager;
import eu.pb4.trinkets.impl.client.slot.ClientTrinketSlotState;
import eu.pb4.trinkets.impl.payload.ToggleCosmeticModePayload;
import eu.pb4.trinkets.impl.payload.ToggleVisibilityPayload;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class TrinketScreenManager {
	public static final Identifier MORE_SLOTS = Identifier.fromNamespaceAndPath("trinkets", "textures/gui/more_slots.png");
	public static final Identifier MORE_SLOTS_INDICATOR_HORIZONTAL = Identifier.fromNamespaceAndPath("trinkets", "container/more_slots_indicator_horizontal");
	public static final Identifier MORE_SLOTS_INDICATOR_HORIZONTAL_COMPATIBLE_HIGHLIGHT = Identifier.fromNamespaceAndPath("trinkets", "container/more_slots_indicator_horizontal_compatible_highlight");
	public static final Identifier MORE_SLOTS_INDICATOR_VERTICAL = Identifier.fromNamespaceAndPath("trinkets", "container/more_slots_indicator_vertical");
	public static final Identifier MORE_SLOTS_INDICATOR_VERTICAL_STANDALONE = Identifier.fromNamespaceAndPath("trinkets", "container/more_slots_indicator_vertical_standalone");
	public static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot");

	public static final Identifier COMPATIBLE_SLOT_HIGHLIGHT_TEXTURE = Identifier.fromNamespaceAndPath("trinkets", "container/compatible_slot_highlight");
	public static final Identifier DECORATIVE_SLOT_OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath("trinkets", "container/decorative_slot_overlay");

	private static WeakReference<TrinketScreen> currentScreen;

	public static final WidgetSprites VISIBILITY_TOGGLE_SPRITES = new WidgetSprites(
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/visibility_button"),
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/visibility_button_disabled"),
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/visibility_button_highlighted"),
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/visibility_button_disabled_highlighted")
	);

	public static final WidgetSprites COSMETIC_TOGGLE_SPRITES = new WidgetSprites(
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/cosmetic_slot_button"),
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/regular_slot_button"),
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/cosmetic_slot_button_highlighted"),
			Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "widget/regular_slot_button_highlighted")
	);

	public static final int VISIBILITY_BUTTON_SIZE = 5;

	public static void init(TrinketScreen screen) {
		currentScreen = new WeakReference<>(screen);
	}

	public static void close() {
		init(null);
	}

	public static void removeSelections() {
		TrinketsClient.activeGroup = null;
		TrinketsClient.quickMoveGroup = null;
	}

	public static void update(float mouseX, float mouseY) {
		TrinketScreen currentScreen = getCurrentScreen();
		if (currentScreen == null) {
			return;
		}

		if (currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().update(currentScreen, mouseX, mouseY);
		}
	}

	public static void tick() {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().tick();
		}
	}

	public static void drawForeground(GuiGraphicsExtractor context) {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().drawForeground(currentScreen, context);
		}
	}

	public static void drawBackground(GuiGraphicsExtractor context) {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().drawBackground(currentScreen, context);
		}
	}

	public static boolean isClickInsideTrinketBounds(double mouseX, double mouseY) {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			return state.getScreenBackend().isClickInsideTrinketBounds(currentScreen, mouseX, mouseY);
		}
		return false;
	}

	static void tryUpdateTrinketsSlot() {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().tryUpdateTrinketsSlot(currentScreen);
		}
	}

	private static TrinketScreen getCurrentScreen() {
		if (currentScreen == null) {
			return null;
		}
		return currentScreen.get();
	}

	public static void setupSlotTooltip(GuiGraphicsExtractor graphics, AbstractContainerScreen menu, int leftPos, int topPos, Slot slot, TrinketSlot trinketSlot, int mouseX, int mouseY) {
		List<Component> text;

		if (TrinketsConfig.serverSyncedGameplay.equipmentHiding && isAtVisiblityToggle(leftPos, topPos, slot, mouseX, mouseY)) {
			text = List.of(Component.translatable("button.trinkets.toggle_visiblity"));
		} else if (trinketSlot.isDecorativeMode()) {
			text = List.of( trinketSlot.getType().getTranslation(),
					Component.translatable("text.trinkets.slot.cosmetic_slot").setStyle(Style.EMPTY.withColor(TextColor.DARK_PURPLE).withItalic(true))
			);
		} else {
			text = List.of(trinketSlot.getType().getTranslation());
		}

		graphics.setComponentTooltipForNextFrame(menu.getFont(), text, mouseX, mouseY);
	}

	public static void drawSlotExtrasFirstDraw(AbstractContainerScreen screen, int slotId, Slot slot, TrinketInventoryMenu trinketMenu, GuiGraphicsExtractor context) {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().drawSlotExtrasFirstDraw(screen, slotId, slot, trinketMenu, context);
		}
	}

	public static void drawSlotExtrasLateDraw(AbstractContainerScreen screen, int slotId, Slot slot, TrinketSlot trinketSlot, GuiGraphicsExtractor context) {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().drawSlotExtrasLateDraw(screen, slotId, slot, trinketSlot, context);
		}
	}

	public static void drawSlotExtrasCommonHead(AbstractContainerScreen screen, GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot, int mouseX, int mouseY) {
		if (TrinketsConfig.instance.highlightCompatibleSlots && !slot.getItem().isEmpty() && slot instanceof TrinketSlot trinketSlot) {
			var stack = screen.getMenu().getCarried();

			if (!stack.isEmpty() && TrinketSlot.canInsert(stack, trinketSlot.getAccess(), trinketSlot.getOwner()) && slot.isHighlightable()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COMPATIBLE_SLOT_HIGHLIGHT_TEXTURE, slot.x - 4, slot.y - 4, 24, 24);
			}
		}

		if (slot instanceof TrinketSlot trinketSlot && slot.getItem().isEmpty() && !trinketSlot.getTrinketGhostItem().isEmpty()) {
			graphics.item(trinketSlot.getTrinketGhostItem(), slot.x, slot.y);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, slot.x - 1, slot.y - 1, 18, 18, 0xAAFFFFFF);
		}

		if (slot instanceof TrinketSlot trinketSlot && trinketSlot.isDecorativeMode()) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DECORATIVE_SLOT_OVERLAY_TEXTURE, slot.x - 4, slot.y - 4, 24, 24);
		}
	}

	public static void drawSlotExtrasCommonTail(AbstractContainerScreen screen, GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot, int mouseX, int mouseY) {
		if (TrinketsConfig.instance.highlightCompatibleSlots && slot.getItem().isEmpty() && slot instanceof TrinketSlot trinketSlot) {
			var stack = screen.getMenu().getCarried();

			if (!stack.isEmpty() && TrinketSlot.canInsert(stack, trinketSlot.getAccess(), trinketSlot.getOwner()) && slot.isHighlightable()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COMPATIBLE_SLOT_HIGHLIGHT_TEXTURE, slot.x - 4, slot.y - 4, 24, 24);
			}
		}

		if (slot instanceof TrinketSlot trinketSlot && canHideEquipment(slot.getItem(), trinketSlot.getTrinketGhostItem()) ) {
			var x = slot.x + 16 - VISIBILITY_BUTTON_SIZE;
			var y = slot.y;
			var isAt = mouseX < leftPos + x + VISIBILITY_BUTTON_SIZE && mouseY < topPos + y + VISIBILITY_BUTTON_SIZE && mouseX >= leftPos + x && mouseY >= topPos + y;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TrinketScreenManager.VISIBILITY_TOGGLE_SPRITES.get(trinketSlot.getAccess().isVisible(), isAt), x, y, VISIBILITY_BUTTON_SIZE, VISIBILITY_BUTTON_SIZE, -1);
		}
	}

	private static boolean isAtVisiblityToggle(int leftPos, int topPos, Slot slot, double ex, double ey) {
		var x = leftPos + slot.x + 16 - VISIBILITY_BUTTON_SIZE;
		var y = topPos + slot.y;
		return ex < x + VISIBILITY_BUTTON_SIZE && ey < y + VISIBILITY_BUTTON_SIZE && ex >= x && ey >= y;
	}

	public static boolean handleModifiedSlotClick(AbstractContainerScreen instance, int leftPos, int topPos, Slot slot, int slotId, int buttonNum, ContainerInput containerInput, MouseButtonEvent event) {
		if (slot instanceof TrinketSlot trinketSlot && canHideEquipment(slot.getItem(), trinketSlot.getTrinketGhostItem())  && trinketSlot.getAccess().inventory() instanceof TrinketInventoryImpl inventory) {
			if (isAtVisiblityToggle(leftPos, topPos,  slot, event.x(), event.y())) {
				var value = !trinketSlot.getAccess().isVisible();
				inventory.setVisible(trinketSlot.getAccess().index(), value);
				AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
				Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new ToggleVisibilityPayload(trinketSlot.getAccess().reference(), value)));
				return true;
			}
		}
		return false;
	}

	private static boolean canHideEquipment(ItemStack item, ItemStack ghostItem) {
		return TrinketsConfig.serverSyncedGameplay.equipmentHiding && (
				(!item.isEmpty() && (TrinketRendererRegistry.hasRenderer(item.getItem()) || !ClientTrinketsManager.INSTANCE.getResolved(item).isEmpty()))
						|| (!ghostItem.isEmpty() && (TrinketRendererRegistry.hasRenderer(ghostItem.getItem()) || !ClientTrinketsManager.INSTANCE.getResolved(ghostItem).isEmpty())));
	}

	public static AbstractWidget createToggleDecorativeModeButton(TrinketScreen screen, IntSupplier leftPos, int topPos) {
		var leftOffsetPos = 3 + (screen instanceof CreativeModeInventoryScreen ? 71 : 25);
		topPos += 3 + (screen instanceof CreativeModeInventoryScreen ? 6 + 40 - 10 : 8 + 65 - 10 + 2);

		return new CosmeticButton(() -> leftPos.getAsInt() + leftOffsetPos, topPos, screen, Component.empty());
	}

	private static final class CosmeticButton extends Button {
		private final TrinketScreen screen;
		private final IntSupplier xUpdater;

		public CosmeticButton(final IntSupplier x, final int y, TrinketScreen screen, final Component message) {
			super(x.getAsInt(), y, 8, 8, message, null, DEFAULT_NARRATION);
			this.xUpdater = x;
			this.screen = screen;
			this.updateTooltip();
		}

		private void updateTooltip() {
			this.setTooltip(Tooltip.create(Component.translatable("button.trinkets.switch_to_"
					+ (!screen.trinkets$getHandler().trinkets$isCosmeticMode() ? "cosmetic" : "regular") + "_slots")));
		}

		@Override
		public void onPress(InputWithModifiers input) {
			var mode = !screen.trinkets$getHandler().trinkets$isCosmeticMode();
			screen.trinkets$getHandler().trinkets$setCosmeticMode(mode);
			Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new ToggleCosmeticModePayload(mode)));
			this.updateTooltip();
		}

		public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
			this.setX(this.xUpdater.getAsInt());
			Identifier sprite = COSMETIC_TOGGLE_SPRITES.get(!screen.trinkets$getHandler().trinkets$isCosmeticMode(), this.isHovered());
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
		}
	}
}
