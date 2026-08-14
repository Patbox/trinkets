package eu.pb4.trinkets.impl.client;

import java.lang.ref.WeakReference;
import java.util.List;

import eu.pb4.trinkets.impl.Point;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.client.slot.ClientTrinketSlotState;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import eu.pb4.trinkets.impl.TrinketsConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;

@Environment(EnvType.CLIENT)
public class TrinketScreenManager {
	public static final Identifier MORE_SLOTS = Identifier.fromNamespaceAndPath("trinkets", "textures/gui/more_slots.png");
	public static final Identifier MORE_SLOTS_INDICATOR_HORIZONTAL = Identifier.fromNamespaceAndPath("trinkets", "container/more_slots_indicator_horizontal");
	public static final Identifier MORE_SLOTS_INDICATOR_VERTICAL = Identifier.fromNamespaceAndPath("trinkets", "container/more_slots_indicator_vertical");
	public static final Identifier MORE_SLOTS_INDICATOR_VERTICAL_STANDALONE = Identifier.fromNamespaceAndPath("trinkets", "container/more_slots_indicator_vertical_standalone");
	public static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot");
	private static WeakReference<TrinketScreen> currentScreen;


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
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().setupSlotTooltip(graphics, menu, leftPos, topPos, slot, trinketSlot, mouseX, mouseY);
		}
	}

	public static void drawSlotExtrasFirstDraw(int slotId, Slot slot, TrinketInventoryMenu trinketMenu, GuiGraphicsExtractor context) {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().drawSlotExtrasFirstDraw(slotId, slot, trinketMenu, context);
		}
	}

	public static void drawSlotExtrasLateDraw(Slot slot, TrinketSlot trinketSlot, GuiGraphicsExtractor context) {
		var currentScreen = getCurrentScreen();

		if (currentScreen != null && currentScreen.trinkets$getSlotState() instanceof ClientTrinketSlotState state) {
			state.getScreenBackend().drawSlotExtrasLateDraw(slot, trinketSlot, context);
		}
	}
}
