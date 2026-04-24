package eu.pb4.trinkets.impl.platform.neo;

import eu.pb4.trinkets.impl.client.TrinketsConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = "trinkets_updated", dist = Dist.CLIENT)
public class TrinketsNeoClientMod {
    public TrinketsNeoClientMod(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (_, previousScreen) -> new TrinketsConfigScreen(previousScreen));
    }
}
