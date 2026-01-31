package eu.pb4.trinkets;

import eu.pb4.trinkets.api.event.TrinketEquipCallback;
import eu.pb4.trinkets.api.event.TrinketUnequipCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsTest implements ModInitializer {

	public static final String MOD_ID = "trinkets-testmod";
	public static final Logger LOGGER = LogManager.getLogger();
	public static Item TEST_TRINKET;
	public static Item TEST_TRINKET_2;

	@Override
	public void onInitialize() {
		LOGGER.info("[Trinkets Testmod] test mod was initialized!");
		TEST_TRINKET = Registry.register(BuiltInRegistries.ITEM, identifier("test"), new TestTrinket(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, identifier("test"))).stacksTo(1).durability(100)));
		TEST_TRINKET_2 = Registry.register(BuiltInRegistries.ITEM, identifier("test2"), new TestTrinket2(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, identifier("test2"))).stacksTo(1)));
		TrinketEquipCallback.EVENT.register(((stack, slot, entity) -> {
			if(stack.is(TEST_TRINKET_2)){
				entity.level().playSound(null, entity.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1f, 1f);
			}
		}));
		TrinketUnequipCallback.EVENT.register(((stack, slot, entity) -> {
			if(stack.is(TEST_TRINKET_2)){
				entity.level().playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5f, 1f);
			}
		}));
	}

	private static Identifier identifier(String id) {
		return Identifier.fromNamespaceAndPath(MOD_ID, id);
	}
}