package eu.pb4.trinkets.api;

import eu.pb4.trinkets.impl.SlotGroupImpl;
import eu.pb4.trinkets.impl.SlotTypeImpl;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.NonExtendable
public interface SlotGroup {
    int slotId();
    int order();
    String name();
    Map<String, SlotType> slots();
}
