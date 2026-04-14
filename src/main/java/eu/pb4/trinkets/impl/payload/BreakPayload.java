package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.api.TrinketSlotReference;
import eu.pb4.trinkets.impl.TrinketsNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record BreakPayload(int entityId, TrinketSlotReference reference) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, BreakPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			BreakPayload::entityId,
			TrinketSlotReference.STREAM_CODEC,
			BreakPayload::reference,
			BreakPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.BREAK;
	}
}
