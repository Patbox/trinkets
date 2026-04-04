package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.impl.SlotGroupImpl;
import eu.pb4.trinkets.impl.TrinketsNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public record SyncSlotsPayload(Map<EntityType<?>, Map<String, SlotGroupImpl>> map) implements CustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSlotsPayload> CODEC = ByteBufCodecs.map(
            (IntFunction<Map<EntityType<?>, Map<String, SlotGroupImpl>>>) HashMap::new,
            ByteBufCodecs.registry(Registries.ENTITY_TYPE),
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, SlotGroupImpl.STREAM_CODEC)
    ).map(SyncSlotsPayload::new, SyncSlotsPayload::map);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TrinketsNetwork.SYNC_SLOTS;
    }
}