package eu.pb4.trinkets.impl.client.slot;

import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public interface TrinketScreenManagerBackend {
    void update(TrinketScreen currentScreen, float mouseX, float mouseY);

    void tick();

    void drawForeground(TrinketScreen currentScreen, GuiGraphicsExtractor context);

    void drawBackground(TrinketScreen currentScreen, GuiGraphicsExtractor context);

    boolean isClickInsideTrinketBounds(TrinketScreen currentScreen, double mouseX, double mouseY);

    void tryUpdateTrinketsSlot(TrinketScreen currentScreen);

    void drawSlotExtrasFirstDraw(AbstractContainerScreen screen, int slotId, Slot slot, TrinketInventoryMenu trinketMenu, GuiGraphicsExtractor context);

    void drawSlotExtrasLateDraw(AbstractContainerScreen screen, int slotId, Slot slot, TrinketSlot trinketSlot, GuiGraphicsExtractor context);

    List<Rect2i> getExclusionAreas(TrinketScreen trinketScreen);
}
