package eu.pb4.trinkets.impl;

import com.google.common.collect.ImmutableMap;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
    public Map<String, SlotType> slots() {
        return ImmutableMap.copyOf(this.slotsImpl);
    }
}
