package eu.pb4.trinkets.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public record TrinketSlotReference(String slot, int index) implements StringRepresentable {
    public static final StreamCodec<FriendlyByteBuf, TrinketSlotReference> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TrinketSlotReference::slot,
            ByteBufCodecs.VAR_INT, TrinketSlotReference::index,
            TrinketSlotReference::new
    );

    public TrinketSlotReference(SlotType slotType, int index) {
        this(slotType.getId(), index);
    }

    public String getSerializedName() {
        return this.slot + "@" + index;
    }

    public String getAsIdentifierPath() {
        return this.slot + "/" + index;
    }
}