package eu.pb4.trinkets.impl.client.slot.sidebar;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.impl.Point;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsConfig;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import eu.pb4.trinkets.impl.client.slot.TrinketScreenManagerBackend;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

import static eu.pb4.trinkets.impl.client.TrinketScreenManager.*;

@Environment(EnvType.CLIENT)
public class SidebarTrinketScreenManager implements TrinketScreenManagerBackend {
    public static final SidebarTrinketScreenManager INSTANCE = new SidebarTrinketScreenManager();

    @Override
    public void update(TrinketScreen currentScreen, float mouseX, float mouseY) {

    }

    @Override
    public void tick() {

    }

    private static void drawTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, int u, int v, int width, int height) {
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
    }

    @Override
    public void drawForeground(TrinketScreen currentScreen, GuiGraphicsExtractor context) {

    }

    @Override
    public void drawBackground(TrinketScreen currentScreen, GuiGraphicsExtractor context) {
        var handler = (SidebarTrinketSlotStateImpl) currentScreen.trinkets$getHandler().trinkets$getSlotState();
        int x = currentScreen.trinkets$getX();
        int y = currentScreen.trinkets$getY();

        if (currentScreen instanceof AbstractContainerScreen<?> screen) {
            for (var slot : screen.getMenu().slots) {
                if (slot instanceof TrinketSlot trinketSlot && !trinketSlot.renderAfterRegularSlots()) {
                    if (!currentScreen.trinkets$isRecipeBookOpen() || slot.x > 0) {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, slot.x + x - 1, slot.y + y - 1, 18, 18);
                    }
                }
            }
        }

        if (currentScreen.trinkets$isRecipeBookOpen()) {
            return;
        }
        var maxHeight = TrinketsConfig.instance.sidebarHeight;

        int width = handler.width();
        int height = handler.lastHeight();
        if (height == 0) {
            height = maxHeight;
            width--;
        }

        drawTexture(context, MORE_SLOTS, x + 3, y, 7, 26, 1, 7);
        // Repeated tops and bottoms
        for (int i = 0; i < width; i++) {
            drawTexture(context, MORE_SLOTS, x - 15 - 18 * i, y, 7, 26, 18, 7);
            drawTexture(context, MORE_SLOTS, x - 15 - 18 * i, y + maxHeight * 18 + 7, 7, 51, 18, 7);
        }
        // Top and bottom
        drawTexture(context, MORE_SLOTS, x - 15 - 18 * width, y, 7, 26, 18, 7);
        drawTexture(context, MORE_SLOTS, x - 15 - 18 * width, y + 7 + 18 * height, 7, 51, 18, 7);
        // Corners
        drawTexture(context, MORE_SLOTS, x - 22 - 18 * width, y, 0, 26, 7, 7);
        drawTexture(context, MORE_SLOTS, x - 22 - 18 * width, y + 7 + 18 * height, 0, 51, 7, 7);
        // Outer sides
        for (int i = 0; i < height; i++) {
            drawTexture(context, MORE_SLOTS, x - 22 - 18 * width, y + 7 + 18 * i, 0, 34, 7, 18);
        }

        // Inner sides
        if (width > 0) {
            for (int i = height; i < TrinketsConfig.instance.sidebarHeight; i++) {
                drawTexture(context, MORE_SLOTS, x - 4 - 18 * width, y + 7 + 18 * i, 0, 34, 7, 18);
            }
        }

        if (width > 0 && height < TrinketsConfig.instance.sidebarHeight) {
            // Bottom corner
            drawTexture(context, MORE_SLOTS, x - 4 - 18 * width, y + maxHeight * 18 + 7, 0, 51, 7, 7);
            // Inner corner
            drawTexture(context, MORE_SLOTS, x - 4 - 18 * width, y + 7 + 18 * height, 0, 58, 7, 7);
        }
        if (width > 0 || height == TrinketsConfig.instance.sidebarHeight) {
            // Inner corner
            drawTexture(context, MORE_SLOTS, x, y + maxHeight * 18 + 7, 0, 58, 3, 7);
        }

        if (width == 0 && height <= TrinketsConfig.instance.sidebarHeight) {
            // Inner corner
            drawTexture(context, MORE_SLOTS, x, y + height * 18 + 7, 0, 58, 3, 7);
        }
    }

    @Override
    public boolean isClickInsideTrinketBounds(TrinketScreen currentScreen, double mouseX, double mouseY) {
        var handler = (SidebarTrinketSlotStateImpl) currentScreen.trinkets$getHandler().trinkets$getSlotState();
        if (currentScreen.trinkets$getFocusedSlot() instanceof TrinketSlot) {
            return true;
        }

        int x = currentScreen.trinkets$getX();
        int y = currentScreen.trinkets$getY();
        int mx = (int) (Math.round(mouseX) - x);
        int my = (int) (Math.round(mouseY) - y);

        if (currentScreen.trinkets$isRecipeBookOpen()) {
            return false;
        }
        int width = handler.width();
        int height = handler.lastHeight();

        if (width > 0) {
            if (new Rect2i(-4 - 18 * width, 0, 7 + 18 * width, 14 + 18 * TrinketsConfig.instance.sidebarHeight).contains(mx, my)) {
                return true;
            }
        }
        if (height > 0) {
            return new Rect2i(-22 - 18 * width, 0, 25, 14 + 18 * height).contains(mx, my);
        }
        return false;
    }

    @Override
    public void tryUpdateTrinketsSlot(TrinketScreen currentScreen) {
        currentScreen.trinkets$updateTrinketSlots();
    }

    @Override
    public void setupSlotTooltip(GuiGraphicsExtractor graphics, AbstractContainerScreen menu, int leftPos, int topPos, Slot slot, TrinketSlot trinketSlot, int mouseX, int mouseY) {
        var text = trinketSlot.getType().getTranslation();
        //var slotX = slot.x + leftPos - mc.font.width(text) / 2 - 4;
        //var slotY = slot.y + topPos - 2;
        var slotX = mouseX;
        var slotY = mouseY;


        graphics.setTooltipForNextFrame(text, slotX, slotY);
    }

    @Override
    public void drawSlotExtrasFirstDraw(int slotId, Slot slot, TrinketInventoryMenu trinketMenu, GuiGraphicsExtractor context) {

    }

    @Override
    public void drawSlotExtrasLateDraw(Slot slot, TrinketSlot trinketSlot, GuiGraphicsExtractor context) {

    }

    @Override
    public List<Rect2i> getExclusionAreas(TrinketScreen trinketScreen) {
        if (trinketScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen &&
                !creativeInventoryScreen.isInventoryOpen()) {
            return List.of();
        }
        List<Rect2i> rects = new ArrayList<>();
        int x = trinketScreen.trinkets$getX();
        int y = trinketScreen.trinkets$getY();
        var handler = (SidebarTrinketSlotStateImpl) trinketScreen.trinkets$getHandler().trinkets$getSlotState();
        if (trinketScreen.trinkets$isRecipeBookOpen()) {
            return List.of();
        }

        var maxHeight = TrinketsConfig.instance.sidebarHeight;

        int width = handler.width();
        int height = handler.lastHeight();
        if (width > 0) {
            rects.add(new Rect2i(-4 - 18 * width + x, y, 7 + 18 * width, 14 + 18 * maxHeight));
        }

        if (height > 0) {
            rects.add(new Rect2i(-22 - 18 * width + x, y, 25, 14 + 18 * height));
        }
        return rects;
    }
}
