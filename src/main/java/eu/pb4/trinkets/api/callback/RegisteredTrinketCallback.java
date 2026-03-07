package eu.pb4.trinkets.api.callback;

import com.mojang.serialization.MapCodec;

public interface RegisteredTrinketCallback extends TrinketCallback {
	<T extends RegisteredTrinketCallback> MapCodec<T> codec();
}