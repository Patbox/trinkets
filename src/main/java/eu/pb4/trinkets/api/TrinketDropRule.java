package eu.pb4.trinkets.api;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TrinketDropRule implements StringRepresentable {
    KEEP("keep"), DROP("drop"), DESTROY("destroy"), DEFAULT("default");

    private final String name;

    private TrinketDropRule(String name) {
        this.name = name;
    }

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
        return this.name;
    }
}
