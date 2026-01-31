package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.impl.TrinketsNetwork;
import eu.pb4.trinkets.api.TrinketSaveData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record SyncInventoryPayload(int entityId, Map<String, ItemStack> contentUpdates, Map<String, TrinketSaveData.Metadata> inventoryUpdates) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncInventoryPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			SyncInventoryPayload::entityId,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ItemStack.OPTIONAL_STREAM_CODEC),
			SyncInventoryPayload::contentUpdates,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, TrinketSaveData.Metadata.PACKET_CODEC_PERSISTENT_ONLY),
			SyncInventoryPayload::inventoryUpdates,
			SyncInventoryPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.SYNC_INVENTORY;
	}
}
