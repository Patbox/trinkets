package eu.pb4.trinkets.impl;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.yumi.mc.core.api.YumiMods;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TrinketsConfig {
    private static final Path CONFIG_PATH = YumiMods.get().getConfigDirectory().resolve("trinkets.json");
    public static TrinketsConfig instance = new TrinketsConfig();

    @SerializedName("show_slot_indicator")
    public boolean showSlotsIndicator = false;
    @SerializedName("render_trinkers_in_first_person")
    public boolean renderFirstPersonHand = false;
    @SerializedName("sidebar_trinkets_slots")
    public boolean sidebarTrinketsSlots = false;
    @SerializedName("sidebar_height")
    public int sidebarHeight = 4;

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                var gson = new GsonBuilder().disableHtmlEscaping().create();
                var config = gson.fromJson(Files.readString(CONFIG_PATH), TrinketsConfig.class);
                if (config != null) {
                    instance = config;
                    save();
                }
            } else {
                save();
            }
        } catch (Throwable e) {
            TrinketsMain.LOGGER.warn("Failed to load Trinkets config!", e);
        }
    }

    public static void save() {
        try {
            var gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
            Files.writeString(CONFIG_PATH, gson.toJson(instance), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Throwable e) {
            TrinketsMain.LOGGER.warn("Failed to save Trinkets config!", e);
        }
    }
}
