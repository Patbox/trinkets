package eu.pb4.trinkets.api.callback;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.impl.TrinketsMain;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;


public interface RegisteredTrinketCallback extends TrinketCallback {
	ResourceKey<Registry<Codec<RegisteredTrinketCallback>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "trinket_callbacks"));
	Registry<Codec<RegisteredTrinketCallback>> REGISTRY = FabricRegistryBuilder.create(REGISTRY_KEY).buildAndRegister();

	<T extends RegisteredTrinketCallback> MapCodec<T> codec();
}