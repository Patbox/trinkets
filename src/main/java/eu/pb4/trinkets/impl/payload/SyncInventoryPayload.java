package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.api.TrinketSlotReference;
import eu.pb4.trinkets.impl.TrinketsNetwork;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record SyncInventoryPayload(int entityId,
								   Map<TrinketSlotReference, ItemStack> contentUpdates,
								   Map<String, Integer> inventorySize,
								   Map<String, BitSet> changedVisibility) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncInventoryPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			SyncInventoryPayload::entityId,
			ByteBufCodecs.map(HashMap::new, TrinketSlotReference.STREAM_CODEC, ItemStack.OPTIONAL_STREAM_CODEC),
			SyncInventoryPayload::contentUpdates,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
			SyncInventoryPayload::inventorySize,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.LONG_ARRAY.map(BitSet::valueOf, BitSet::toLongArray)),
			SyncInventoryPayload::changedVisibility,
			SyncInventoryPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.SYNC_INVENTORY;
	}
}
