package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.api.TrinketSlotReference;
import eu.pb4.trinkets.impl.TrinketsNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record ToggleVisibilityPayload(TrinketSlotReference reference, boolean value) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleVisibilityPayload> CODEC = StreamCodec.composite(
			TrinketSlotReference.STREAM_CODEC,
			ToggleVisibilityPayload::reference,
			ByteBufCodecs.BOOL,
			ToggleVisibilityPayload::value,
			ToggleVisibilityPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.TOGGLE_VISIBILITY;
	}
}
