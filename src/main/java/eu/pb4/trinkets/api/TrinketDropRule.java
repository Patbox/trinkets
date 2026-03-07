package eu.pb4.trinkets.api;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TrinketDropRule implements StringRepresentable {
    KEEP, DROP, DESTROY, DEFAULT;

    static public boolean has(String name) {
        TrinketDropRule[] rules = TrinketDropRule.values();

        for (TrinketDropRule rule : rules) {

            if (rule.toString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
