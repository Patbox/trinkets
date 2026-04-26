package eu.pb4.trinkets.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public record TrinketSlotReference(String slot, int index) implements StringRepresentable {
    public static final StreamCodec<FriendlyByteBuf, TrinketSlotReference> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TrinketSlotReference::slot,
            ByteBufCodecs.VAR_INT, TrinketSlotReference::index,
            TrinketSlotReference::new
    );

    public static final Codec<TrinketSlotReference> CODEC = Codec.STRING.comapFlatMap(TrinketSlotReference::read, TrinketSlotReference::getSerializedName);

    public TrinketSlotReference(SlotType slotType, int index) {
        this(slotType.getId(), index);
    }

    public String getSerializedName() {
        return this.slot + "@" + index;
    }

    public String getAsIdentifierPath() {
        return this.slot + "/" + index;
    }

    @Override
    public @NonNull String toString() {
        return this.getSerializedName();
    }

    private static DataResult<TrinketSlotReference> read(String string) {
        var at = string.indexOf('@');

        if (at == -1) {
            return DataResult.error(() -> "Not a valid trinket slot reference (missing @)...");
        }
        var slot = string.substring(0, at);
        var index = string.substring(at + 1);

        if (!Identifier.isValidPath(slot)) {
            return DataResult.error(() -> "Invalid path!");
        }

        try {
            return DataResult.success(new TrinketSlotReference(slot, Integer.parseInt(index)));
        } catch (Throwable e) {
            return DataResult.error(() -> "Invalid slot id!");
        }
    }
}