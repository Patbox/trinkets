package eu.pb4.trinkets.impl;

import com.google.common.collect.ImmutableMap;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.Slot;

import java.util.HashMap;
import java.util.Map;

public record SlotGroupImpl(String name, int slotId, int order,
                            Map<String, SlotTypeImpl> slotsImpl) implements SlotGroup {

    public static final StreamCodec<FriendlyByteBuf, SlotGroupImpl> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SlotGroupImpl::name,
            ByteBufCodecs.INT, SlotGroupImpl::slotId,
            ByteBufCodecs.INT, SlotGroupImpl::order,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, SlotTypeImpl.STREAM_CODEC), SlotGroupImpl::slotsImpl,
            SlotGroupImpl::new
    );

    @Override
    public boolean isAttachedToSlot(Slot slot) {
        return slot.index == this.slotId;
    }

    @Override
    public boolean hasSlotAttachment() {
        return !TrinketsConfig.instance.sidebarTrinketsSlots && this.slotId != -1;
    }

    @Override
    public Map<String, SlotType> slots() {
        return ImmutableMap.copyOf(this.slotsImpl);
    }

    public static class Builder {
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
