package eu.pb4.trinkets.impl.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public interface CreativeTrinketScreen {
    void trinkets$renderCreative(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks);
}
