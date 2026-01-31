package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.impl.TrinketsNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record BreakPayload(int entityId, String group, String slot, int index) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, BreakPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			BreakPayload::entityId,
			ByteBufCodecs.STRING_UTF8,
			BreakPayload::group,
			ByteBufCodecs.STRING_UTF8,
			BreakPayload::slot,
			ByteBufCodecs.VAR_INT,
			BreakPayload::index,
			BreakPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.BREAK;
	}
}
