package eu.pb4.trinkets.api.client;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.Item;

@Environment(EnvType.CLIENT)
public class TrinketRendererRegistry {
	private static final Map<Item, TrinketRenderer> RENDERERS = new IdentityHashMap<>();

	/**
	 * Registers a trinket renderer for the provided item
	 */
	public static void registerRenderer(Item item, TrinketRenderer trinketRenderer) {
		RENDERERS.put(item, trinketRenderer);
	}

	public static Optional<TrinketRenderer> getRenderer(Item item) {
		return Optional.ofNullable(RENDERERS.get(item));
	}

	public static boolean hasRenderer(Item item) {
		return RENDERERS.containsKey(item);
	}
}
