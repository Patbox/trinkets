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

    class Builder {

        private final String name;
        private final int slotId;
        private final int order;
        private final Map<String, SlotTypeImpl> slots = new HashMap<>();

        public Builder(String name, int slotId, int order) {
            this.name = name;
            this.slotId = slotId;
            this.order = order;
        }

        public Builder addSlot(String name, SlotType slot) {
            this.slots.put(name, (SlotTypeImpl) slot);
            return this;
        }

        public SlotGroupImpl build() {
            return new SlotGroupImpl(name, slotId, order, slots);
        }
    }
}
