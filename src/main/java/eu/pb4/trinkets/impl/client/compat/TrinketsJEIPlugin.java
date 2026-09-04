package eu.pb4.trinkets.impl.client.compat;

import eu.pb4.trinkets.impl.client.TrinketScreen;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class TrinketsJEIPlugin implements IModPlugin {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("trinkets", "exclusions");

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public @NonNull List<Rect2i> getGuiExtraAreas(AbstractContainerScreen containerScreen) {
                return TrinketsExclusionAreas.create(containerScreen);
            }
        });
    }

    @Override
    public Identifier getPluginUid() {
        return ID;
    }
}