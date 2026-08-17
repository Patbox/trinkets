package eu.pb4.trinkets.impl.client.slot.legacy;

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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static eu.pb4.trinkets.impl.client.TrinketScreenManager.*;

@Environment(EnvType.CLIENT)
public class LegacyTrinketScreenManager implements TrinketScreenManagerBackend {
    public static final LegacyTrinketScreenManager INSTANCE = new LegacyTrinketScreenManager();

    public Rect2i currentBounds = new Rect2i(0, 0, 0, 0);
    public Rect2i typeBounds = new Rect2i(0, 0, 0, 0);
    public Rect2i quickMoveBounds = new Rect2i(0, 0, 0, 0);
    public Rect2i quickMoveTypeBounds = new Rect2i(0, 0, 0, 0);
    public SlotGroup group = null;
    public SlotGroup quickMoveGroup = null;

    @Override
    public void update(TrinketScreen currentScreen, float mouseX, float mouseY) {
        var handler = (LegacyTrinketSlotState) currentScreen.trinkets$getSlotState();
        Slot focusedSlot = currentScreen.trinkets$getFocusedSlot();
        int x = currentScreen.trinkets$getX();
        int y = currentScreen.trinkets$getY();
        if (group != null) {
            if (TrinketsClient.activeType != null) {
                if (!typeBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
                    // Attempt to refresh the typeBounds, in case the slot count has changed.
                    int i = handler.getSlotTypes(group).indexOf(TrinketsClient.activeType);
                    if (i >= 0) {
                        var r = handler.getGroupRect(group);
                        Point slotHeight = handler.getSlotHeight(group, i);
                        int height = slotHeight.y();
                        typeBounds = new Rect2i(r.x() + slotHeight.x() - 2, r.y() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
                    }
                    if (!typeBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
                        TrinketsClient.activeType = null;
                    }
                } else if (focusedSlot != null) {
                    if (!(focusedSlot instanceof TrinketSlot ts && ts.getType() == TrinketsClient.activeType)) {
                        TrinketsClient.activeType = null;
                    }
                }
            }
            if (TrinketsClient.activeType == null) {
                if (!currentBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
                    TrinketsClient.activeGroup = null;
                    group = null;
                } else {
                    if (focusedSlot instanceof TrinketSlot ts) {
                        int i = handler.getSlotTypes(group).indexOf(ts.getType());
                        if (i >= 0) {
                            Point slotHeight = handler.getSlotHeight(group, i);
                            if (slotHeight != null) {
                                var r = handler.getGroupRect(group);
                                int height = slotHeight.y();
                                if (height > 1) {
                                    TrinketsClient.activeType = ts.getType();
                                    typeBounds = new Rect2i(r.x() + slotHeight.x() - 2, r.y() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (group == null && quickMoveGroup != null) {
            if (quickMoveTypeBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
                TrinketsClient.activeGroup = quickMoveGroup;
                TrinketsClient.activeType = TrinketsClient.quickMoveType;
                int i = handler.getSlotTypes(TrinketsClient.activeGroup).indexOf(TrinketsClient.activeType);
                if (i >= 0) {
                    Point slotHeight = handler.getSlotHeight(TrinketsClient.activeGroup, i);
                    if (slotHeight != null) {
                        var r = ((LegacyTrinketSlotState) currentScreen.trinkets$getSlotState()).getGroupRect(TrinketsClient.activeGroup);
                        int height = slotHeight.y();
                        if (height > 1) {
                            typeBounds = new Rect2i(r.x() + slotHeight.x() - 2, r.y() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
                        }
                    }
                }
                TrinketsClient.quickMoveGroup = null;
            } else if (quickMoveBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
                TrinketsClient.activeGroup = quickMoveGroup;
                TrinketsClient.quickMoveGroup = null;
            }
        }

        if (group == null) {
            Minecraft client = Minecraft.getInstance();
            for (SlotGroup g : SlotGroup.getEntityGroups(client.player).values()) {
                var r = ((LegacyTrinketSlotState) currentScreen.trinkets$getSlotState()).getGroupRect(g);
                if (r.x() < 0 && currentScreen.trinkets$isRecipeBookOpen()) {
                    continue;
                }
                if (r.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
                    if (!(currentScreen.trinkets$isNarrow() && currentScreen.trinkets$isRecipeBookOpen())) {
                        TrinketsClient.activeGroup = g;
                        TrinketsClient.quickMoveGroup = null;
                        break;
                    }
                }
            }
        }

        if (group != TrinketsClient.activeGroup) {
            group = TrinketsClient.activeGroup;

            if (group != null) {
                int slotsWidth = handler.getSlotWidth(group) + 1;
                if (!(group.hasSlotAttachment() && !handler.forceSidebar())) slotsWidth -= 1;
                var r = ((LegacyTrinketSlotState) currentScreen.trinkets$getSlotState()).getGroupRect(group);
                currentBounds = new Rect2i(0, 0, 0, 0);

                if (r != null) {
                    int l = (slotsWidth - 1) / 2 * 18;

                    if (slotsWidth > 1) {
                        currentBounds = new Rect2i(r.x() - l - 3, r.y() - 3, slotsWidth * 18 + 5, 23);
                    } else {
                        currentBounds = new Rect2i(r.x(), r.y(), r.width(), r.height());
                    }

                    if (focusedSlot instanceof TrinketSlot ts) {
                        int i = handler.getSlotTypes(group).indexOf(ts.getType());
                        if (i >= 0) {
                            Point slotHeight = handler.getSlotHeight(group, i);
                            if (slotHeight != null) {
                                int height = slotHeight.y();
                                if (height > 1) {
                                    TrinketsClient.activeType = ts.getType();
                                    typeBounds = new Rect2i(r.x() + slotHeight.x() - 2, r.y() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (quickMoveGroup != TrinketsClient.quickMoveGroup) {
            quickMoveGroup = TrinketsClient.quickMoveGroup;

            if (quickMoveGroup != null) {
                int slotsWidth = handler.getSlotWidth(quickMoveGroup) + 1;

                if (!(quickMoveGroup.hasSlotAttachment() && !handler.forceSidebar())) slotsWidth -= 1;
                var r = ((LegacyTrinketSlotState) currentScreen.trinkets$getSlotState()).getGroupRect(quickMoveGroup);
                quickMoveBounds = new Rect2i(0, 0, 0, 0);

                if (r != null) {
                    int l = (slotsWidth - 1) / 2 * 18;
                    quickMoveBounds = new Rect2i(r.x() - l - 5, r.y() - 5, slotsWidth * 18 + 8, 26);
                    if (TrinketsClient.quickMoveType != null) {
                        int i = handler.getSlotTypes(quickMoveGroup).indexOf(TrinketsClient.quickMoveType);
                        if (i >= 0) {
                            Point slotHeight = handler.getSlotHeight(quickMoveGroup, i);
                            if (slotHeight != null) {
                                int height = slotHeight.y();
                                quickMoveTypeBounds = new Rect2i(r.x() + slotHeight.x() - 2, r.y() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        if (TrinketsClient.quickMoveTimer > 0) {
            TrinketsClient.quickMoveTimer--;

            if (TrinketsClient.quickMoveTimer <= 0) {
                TrinketsClient.quickMoveGroup = null;
            }
        }
    }

    public void drawGroup(TrinketScreen currentScreen, GuiGraphicsExtractor context, SlotGroup group, SlotType type) {
        var handler = (LegacyTrinketSlotState) currentScreen.trinkets$getSlotState();
        context.pose().pushMatrix();
        var r = handler.getGroupRect(group);
        int slotsWidth = handler.getSlotWidth(group) + 1;
        List<Point> slotHeights = handler.getSlotHeights(group);
        List<SlotType> slotTypes = handler.getSlotTypes(group);
        if (!(group.hasSlotAttachment() && !handler.forceSidebar())) slotsWidth -= 1;
        int x = r.x() - 4 - (slotsWidth - 1) / 2 * 18;
        int y = r.y() - 4;
        if (slotsWidth > 1 || type != null) {
            drawTexture(context, MORE_SLOTS, x, y, 0, 0, 4, 26);

            for (int i = 0; i < slotsWidth; i++) {
                drawTexture(context, MORE_SLOTS, x + i * 18 + 4, y, 4, 0, 18, 26);
            }

            drawTexture(context, MORE_SLOTS, x + slotsWidth * 18 + 4, y, 22, 0, 4, 26);
            for (int s = 0; s < slotHeights.size() && s < slotTypes.size(); s++) {
                if (slotTypes.get(s) != type) {
                    continue;
                }
                Point slotHeight = slotHeights.get(s);
                int height = slotHeight.y();
                if (height > 1) {
                    int top = (height - 1) / 2;
                    int bottom = height / 2;
                    int slotX = slotHeight.x() - 4 + r.x();
                    if (height > 2) {
                        drawTexture(context, MORE_SLOTS, slotX, y - top * 18, 0, 0, 26, 4);
                    }

                    for (int i = 1; i < top + 1; i++) {
                        drawTexture(context, MORE_SLOTS, slotX, y - i * 18 + 4, 0, 4, 26, 18);
                    }

                    for (int i = 1; i < bottom + 1; i++) {
                        drawTexture(context, MORE_SLOTS, slotX, y + i * 18 + 4, 0, 4, 26, 18);
                    }

                    drawTexture(context, MORE_SLOTS, slotX, y + 18 + bottom * 18 + 4, 0, 22, 26, 4);
                }
            }


            // The rest of this is just to re-render a portion of the top and bottom slot borders so that corners
            // between slot types on the GUI look nicer
            for (int s = 0; s < slotHeights.size(); s++) {
                Point slotHeight = slotHeights.get(s);
                int height = slotHeight.y();
                if (slotTypes.get(s) != type) {
                    height = 1;
                }
                int slotX = slotHeight.x() + r.x() + 1;
                int top = (height - 1) / 2;
                int bottom = height / 2;
                drawTexture(context, MORE_SLOTS, slotX, y - top * 18 + 1, 4, 1, 16, 3);
                drawTexture(context, MORE_SLOTS, slotX, y + (bottom + 1) * 18 + 4, 4, 22, 16, 3);
            }

            // Because pre-existing slots are not part of the slotHeights list
            if (group.hasSlotAttachment() && !handler.forceSidebar()) {
                drawTexture(context, MORE_SLOTS, r.x() + 1, y + 1, 4, 1, 16, 3);
                drawTexture(context, MORE_SLOTS, r.x() + 1, y + 22, 4, 22, 16, 3);
            }
        } else {
            drawTexture(context, MORE_SLOTS, x + 4, y + 4, 4, 4, 18, 18);
        }

        context.pose().popMatrix();
    }

    private static void drawTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, int u, int v, int width, int height) {
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
    }

    @Override
    public void drawForeground(TrinketScreen currentScreen, GuiGraphicsExtractor context) {
        if (TrinketsClient.activeGroup != null) {
            this.drawGroup(currentScreen, context, TrinketsClient.activeGroup, TrinketsClient.activeType);
        } else if (TrinketsClient.quickMoveGroup != null) {
            this.drawGroup(currentScreen, context, TrinketsClient.quickMoveGroup, TrinketsClient.quickMoveType);
        }
    }

    @Override
    public void drawBackground(TrinketScreen currentScreen, GuiGraphicsExtractor context) {
        TrinketInventoryMenu handler = currentScreen.trinkets$getHandler();
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

        int groupCount = ((LegacyTrinketSlotState) handler.trinkets$getSlotState()).groupCount();
        if (groupCount <= 0 || currentScreen.trinkets$isRecipeBookOpen()) {
            return;
        }
        var maxHeight = TrinketsConfig.instance.sidebarHeight;

        int width = groupCount / maxHeight;
        int height = groupCount % maxHeight;
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
        TrinketInventoryMenu handler = currentScreen.trinkets$getHandler();
        if (currentScreen.trinkets$getFocusedSlot() instanceof TrinketSlot) {
            return true;
        }

        int x = currentScreen.trinkets$getX();
        int y = currentScreen.trinkets$getY();
        int mx = (int) (Math.round(mouseX) - x);
        int my = (int) (Math.round(mouseY) - y);
        if (this.currentBounds.contains(mx, my)) {
            return true;
        }
        int groupCount = ((LegacyTrinketSlotState) handler.trinkets$getSlotState()).groupCount();
        if (groupCount <= 0 || currentScreen.trinkets$isRecipeBookOpen()) {
            return false;
        }
        var maxHeight = TrinketsConfig.instance.sidebarHeight;

        int width = groupCount / 4;
        int height = groupCount % 4;
        if (width > 0) {
            if (new Rect2i(-4 - 18 * width, 0, 7 + 18 * width, 14 + 18 * maxHeight).contains(mx, my)) {
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
        // Refresh the type bounds of the currently open Trinket Group on slot change.
        typeBounds = new Rect2i(0, 0, 0, 0);
        currentScreen.trinkets$updateTrinketSlots();

    }

    @Override
    public void drawSlotExtrasFirstDraw(AbstractContainerScreen screen, int slotId, Slot slot, TrinketInventoryMenu trinketMenu, GuiGraphicsExtractor context) {
        if (slot instanceof TrinketSlot trinketSlot) {
            var g = trinketSlot.getGroup();

            if (!trinketSlot.renderAfterRegularSlots() && slot.isActive() && trinketSlot.getAccess().index() == 0 && TrinketsClient.activeGroup != g && g != null && ((LegacyTrinketSlotState) trinketMenu.trinkets$getSlotState()).getSlotTypes(g).size() > 1) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED,
                        isGroupHighlighted(trinketMenu, g) ? MORE_SLOTS_INDICATOR_HORIZONTAL_COMPATIBLE_HIGHLIGHT : MORE_SLOTS_INDICATOR_HORIZONTAL,
                        slot.x - 8, slot.y - 8, 32, 32);
            }
            if (!trinketSlot.renderAfterRegularSlots() && slot.isActive() && trinketSlot.getAccess().inventory().getContainerSize() > 1 && trinketSlot.getAccess().index() == 0 && TrinketsClient.activeType != trinketSlot.getType()) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, MORE_SLOTS_INDICATOR_VERTICAL_STANDALONE, slot.x - 8, slot.y - 8, 32, 32);
            }
        } else {
            var g = ((LegacyTrinketSlotState) trinketMenu.trinkets$getSlotState()).getGroupAtSlot(slotId);
            if (g != null && this.group != g) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED,
                        isGroupHighlighted(trinketMenu, g) ? MORE_SLOTS_INDICATOR_HORIZONTAL_COMPATIBLE_HIGHLIGHT : MORE_SLOTS_INDICATOR_HORIZONTAL,
                        slot.x - 8, slot.y - 8, 32, 32);
            }
        }
    }

    private boolean isGroupHighlighted(TrinketInventoryMenu trinketMenu, SlotGroup group) {
        var carried = ((AbstractContainerMenu) trinketMenu).getCarried();
        if (carried.isEmpty()) {
            return false;
        }

        var slotTypes = ((LegacyTrinketSlotState) trinketMenu.trinkets$getSlotState()).getSlotTypes(group);
        var att = trinketMenu.trinkets$attachment();

        if (TrinketsConfig.instance.highlightCompatibleSlots) {
            for (var type : slotTypes) {
                var inv = Objects.requireNonNull(att.getInventory(type.getId()));

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (TrinketSlot.canInsert(carried, inv.getSlotAccess(i), att.getEntity())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void drawSlotExtrasLateDraw(AbstractContainerScreen screen, int slotId, Slot slot, TrinketSlot trinketSlot, GuiGraphicsExtractor context) {
        if (TrinketsConfig.instance.showSlotsIndicator && trinketSlot.getAccess().inventory().getContainerSize() > 1 && trinketSlot.getAccess().index() == 0 && TrinketsClient.activeType != trinketSlot.getType()) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, MORE_SLOTS_INDICATOR_VERTICAL, slot.x - 8, slot.y - 8, 32, 32);
        }
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
        TrinketInventoryMenu handler = trinketScreen.trinkets$getHandler();
        int groupCount = ((LegacyTrinketSlotState) handler.trinkets$getSlotState()).groupCount();
        if (groupCount <= 0 || trinketScreen.trinkets$isRecipeBookOpen()) {
            return List.of();
        }

        if (TrinketsClient.activeGroup != null) {
            Rect2i rect = currentBounds;
            rects.add(
                    new Rect2i(rect.getX() + x, rect.getY() + y, rect.getWidth(),
                            rect.getHeight()));
        }
        var maxHeight = TrinketsConfig.instance.sidebarHeight;

        int width = groupCount / maxHeight;
        int height = groupCount % maxHeight;
        if (width > 0) {
            rects.add(new Rect2i(-4 - 18 * width + x, y, 7 + 18 * width, 14 + 18 * maxHeight));
        }

        if (height > 0) {
            rects.add(new Rect2i(-22 - 18 * width + x, y, 25, 14 + 18 * height));
        }
        return rects;
    }
}
