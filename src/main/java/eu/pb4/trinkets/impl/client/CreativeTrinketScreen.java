package eu.pb4.trinkets.impl.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

@Environment(EnvType.CLIENT)
public interface CreativeTrinketScreen {
    void trinkets$renderCreative(GuiGraphics context, int mouseX, int mouseY, float deltaTicks);
}
