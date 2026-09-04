package eu.pb4.trinkets.api.client;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.impl.client.render.TrinketRenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.Item;

@Environment(EnvType.CLIENT)
public class TrinketRendererRegistry {
	public static final Map<Item, TrinketRenderer> LEGACY_RENDERERS = new IdentityHashMap<>();

	/**
	 * Registers a trinket renderer for the provided item
	 */
	public static void registerRenderer(Item item, TrinketRenderer trinketRenderer) {
		TrinketRenderLayer.RENDERERS.put(item, _ -> trinketRenderer);
		LEGACY_RENDERERS.put(item, trinketRenderer);
	}

	/**
	 * Registers a trinket renderer for the provided item
	 */
	public static void registerRenderer(Item item, Function<EntityRendererProvider.Context, TrinketRenderer> trinketRenderer) {
		TrinketRenderLayer.RENDERERS.put(item, trinketRenderer);
	}

	/// @deprecated This method no longer works with dynamically supplied renderers!
	@Deprecated(forRemoval = true)
	public static Optional<TrinketRenderer> getRenderer(Item item) {
		return Optional.ofNullable(LEGACY_RENDERERS.get(item));
	}

	public static boolean hasRenderer(Item item) {
		return TrinketRenderLayer.RENDERERS.containsKey(item);
	}
}
