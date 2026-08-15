package eu.pb4.trinkets.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public record TrinketSlotReference(String slot, int index, boolean cosmetic) implements StringRepresentable {
    public static final StreamCodec<FriendlyByteBuf, TrinketSlotReference> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TrinketSlotReference::slot,
            ByteBufCodecs.VAR_INT, TrinketSlotReference::index,
            ByteBufCodecs.BOOL, TrinketSlotReference::cosmetic,
            TrinketSlotReference::new
    );

    public static final Codec<TrinketSlotReference> CODEC = Codec.STRING.comapFlatMap(TrinketSlotReference::read, TrinketSlotReference::getSerializedName);

    public TrinketSlotReference(String slot, int index) {
        this(slot, index, false);
    }

    public TrinketSlotReference(SlotType slotType, int index) {
        this(slotType.getId(), index, false);
    }

    public String getSerializedName() {
        return this.slot + "@" + index + (cosmetic ? "?cosmetic" : "");
    }

    public String getAsIdentifierPath() {
        return this.slot + "/" + index + (cosmetic ? "/_/cosmetic" : "");
    }

    @Override
    public @NonNull String toString() {
        return this.getSerializedName();
    }

    private static DataResult<TrinketSlotReference> read(String string) {
        var at = string.indexOf('@');
        var q = string.indexOf('?');

        if (at == -1) {
            return DataResult.error(() -> "Not a valid trinket slot reference (missing @)...");
        }
        var slot = string.substring(0, at);
        var index = string.substring(at + 1, q == -1 ? string.length() : q);
        var type = string.substring(q + 1);

        if (!Identifier.isValidPath(slot)) {
            return DataResult.error(() -> "Invalid path!");
        }

        try {
            return DataResult.success(new TrinketSlotReference(slot, Integer.parseInt(index), type.equals("cosmetic")));
        } catch (Throwable e) {
            return DataResult.error(() -> "Invalid slot id!");
        }
    }
}