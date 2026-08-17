package eu.pb4.trinkets.impl;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.yumi.mc.core.api.YumiMods;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TrinketsConfig {
    private static final Path CONFIG_PATH = YumiMods.get().getConfigDirectory().resolve("trinkets.json");
    public static TrinketsConfig instance = new TrinketsConfig();
    public static TrinketsConfig.Gameplay serverSyncedGameplay = new TrinketsConfig.Gameplay();

    public static TrinketsConfig.Gameplay getGameplay(boolean isClient) {
        return isClient ? serverSyncedGameplay : instance.gameplay;
    }

    @SerializedName("render_trinkers_in_first_person")
    public boolean renderFirstPersonHand = false;
    @SerializedName("ui_style")
    public String uiStyle = "default";
    @SerializedName("sidebar_height")
    public int sidebarHeight = 4;
    @SerializedName("show_slot_indicator")
    public boolean showSlotsIndicator = false;
    @SerializedName("show_slot_name_tooltip")
    public boolean showSlotTooltip = false;
    @SerializedName("show_item_tooltip")
    public boolean showItemTooltip = true;
    @SerializedName("highlight_compatible_slots")
    public boolean highlightCompatibleSlots = false;

    @SerializedName("gameplay")
    public Gameplay gameplay = new Gameplay();

    public static class Gameplay {
        @SerializedName("equipment_hiding")
        public boolean equipmentHiding = false;

        @SerializedName("cosmetic_slots")
        public boolean cosmeticSlots = false;
    }


    @Deprecated
    @Expose(serialize = false)
    @SerializedName("sidebar_trinkets_slots")
    private Boolean legacy_sidebarTrinketsSlots = null;

    private void update() {
        if (legacy_sidebarTrinketsSlots != null) {
            uiStyle = "grouped_sidebar";
            legacy_sidebarTrinketsSlots = null;
        }
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                var gson = new GsonBuilder().disableHtmlEscaping().create();
                var config = gson.fromJson(Files.readString(CONFIG_PATH), TrinketsConfig.class);
                if (config != null) {
                    instance = config;
                    instance.update();
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
