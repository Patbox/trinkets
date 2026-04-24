package eu.pb4.trinkets.impl.platform.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import eu.pb4.trinkets.impl.client.TrinketsConfigScreen;

public class ModMenuCompatibility implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TrinketsConfigScreen::new;
    }
}
