package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.impl.TrinketsNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record ToggleCosmeticModePayload(boolean value) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCosmeticModePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			ToggleCosmeticModePayload::value,
			ToggleCosmeticModePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.TOGGLE_COSMETIC_MODE;
	}
}
