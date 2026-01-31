package eu.pb4.trinkets.client;

import eu.pb4.trinkets.TrinketsTest;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;

public class TrinketsTestClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		TrinketRendererRegistry.registerRenderer(TrinketsTest.TEST_TRINKET, (TrinketRenderer) TrinketsTest.TEST_TRINKET);
	}
}
