package eu.pb4.trinkets.impl.payload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.pb4.trinkets.impl.TrinketsConfig;
import eu.pb4.trinkets.impl.TrinketsNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ExtraCodecs;

public record SyncConfigPayload(TrinketsConfig.Gameplay gameplay) implements CustomPacketPayload {
	private static final Gson GSON = new GsonBuilder().create();
	public static final StreamCodec<ByteBuf, SyncConfigPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(
			x -> new SyncConfigPayload(GSON.fromJson(x, TrinketsConfig.Gameplay.class)), x -> GSON.toJson(x.gameplay())
	);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.SYNC_CONFIG;
	}
}
