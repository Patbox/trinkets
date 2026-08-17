package eu.pb4.trinkets.impl.client.slot;

import eu.pb4.trinkets.impl.client.slot.legacy.LegacyTrinketSlotStateImpl;
import eu.pb4.trinkets.impl.client.slot.sidebar.SidebarTrinketSlotStateImpl;
import eu.pb4.trinkets.impl.slots.TrinketSlotState;
import net.minecraft.util.Util;

import java.util.LinkedHashMap;
import java.util.Map;

public interface ClientTrinketSlotState {
    Map<String, TrinketSlotState.Constructor> CONSTRUCTORS = Util.make(new LinkedHashMap<>(), m -> {
        m.put("default", LegacyTrinketSlotStateImpl::classic);
        m.put("grouped_sidebar", LegacyTrinketSlotStateImpl::sidebar);
        m.put("flat_sidebar", SidebarTrinketSlotStateImpl::new);
    });

    TrinketScreenManagerBackend getScreenBackend();
}
