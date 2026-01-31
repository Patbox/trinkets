package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Function3;
import eu.pb4.trinkets.impl.TrinketModifiers;
import eu.pb4.trinkets.impl.TrinketSlotTarget;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.payload.BreakPayload;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class TrinketsApi {
	public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
			.getOrCreate(Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "trinkets"), TrinketComponent.class);
	private static final Map<Identifier, Function3<ItemStack, SlotReference, LivingEntity, TriState>> PREDICATES = new HashMap<>();

	private static final Map<Item, Trinket> TRINKETS = new HashMap<>();
	private static final Trinket DEFAULT_TRINKET;

	/**
	 * Registers a trinket for the provided item, {@link TrinketItem} will do this
	 * automatically.
	 */
	public static void registerTrinket(Item item, Trinket trinket) {
		TRINKETS.put(item, trinket);
	}

	public static Trinket getTrinket(Item item) {
		return TRINKETS.getOrDefault(item, DEFAULT_TRINKET);
	}

	public static Trinket getDefaultTrinket() {
		return DEFAULT_TRINKET;
	}

	/**
	 * @return The trinket component for this entity, if available
	 */
	public static Optional<TrinketComponent> getTrinketComponent(LivingEntity livingEntity) {
		return TRINKET_COMPONENT.maybeGet(livingEntity);
	}

	/**
	 * Called to sync a trinket breaking event with clients. Should generally be
	 * called in the callback of {@link ItemStack#hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer)}
	 */
	public static void onTrinketBroken(ItemStack stack, SlotReference ref, LivingEntity entity) {
		Level world = entity.level();
		if (!world.isClientSide()) {
			BreakPayload packet = new BreakPayload(entity.getId(), ref.inventory().getSlotType().getGroup(), ref.inventory().getSlotType().getName(), ref.index());
			if (entity instanceof ServerPlayer player) {
				ServerPlayNetworking.send(player, packet);
			}
			PlayerLookup.tracking(entity).forEach(watcher -> {
				ServerPlayNetworking.send(watcher, packet);
			});
		}
	}

	/**
	 * @deprecated Use world-sensitive alternative {@link TrinketsApi#getPlayerSlots(Level)}
	 * @return A map of slot group names to slot groups available for players
	 */
	@Deprecated
	public static Map<String, SlotGroup> getPlayerSlots() {
		return getEntitySlots(EntityType.PLAYER);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for players
	 */
	public static Map<String, SlotGroup> getPlayerSlots(Level world) {
		return getEntitySlots(world, EntityType.PLAYER);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for players
	 */
	public static Map<String, SlotGroup> getPlayerSlots(Player player) {
		return getEntitySlots(player);
	}

	/**
	 * @deprecated Use world-sensitive alternative {@link TrinketsApi#getEntitySlots(Level, EntityType)}
	 * @return A map of slot group names to slot groups available for the provided
	 * entity type
	 */
	@Deprecated
	public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
		EntitySlotLoader loader = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
		return loader.getEntitySlots(type);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for the provided
	 * entity type
	 */
	public static Map<String, SlotGroup> getEntitySlots(Level world, EntityType<?> type) {
		EntitySlotLoader loader = world.isClientSide() ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
		return loader.getEntitySlots(type);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for the provided
	 * entity
	 */
	public static Map<String, SlotGroup> getEntitySlots(Entity entity) {
		if (entity != null) {
			return getEntitySlots(entity.level(), entity.getType());
		}
		return ImmutableMap.of();
	}

	/**
	 * Registers a predicate to be referenced in slot data
	 */
	public static void registerTrinketPredicate(Identifier id, Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate) {
		PREDICATES.put(id, predicate);
	}

	public static Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> getTrinketPredicate(Identifier id) {
		return Optional.ofNullable(PREDICATES.get(id));
	}

	public static boolean evaluatePredicateSet(Set<Identifier> set, ItemStack stack, SlotReference ref, LivingEntity entity) {
		TriState state = TriState.DEFAULT;
		for (Identifier id : set) {
			Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> function = getTrinketPredicate(id);
			if (function.isPresent()) {
				state = function.get().apply(stack, ref, entity);
			}
			if (state != TriState.DEFAULT) {
				break;
			}
		}
		return state.get();
	}

	public static Enchantment.EnchantmentDefinition withTrinketSlots(Enchantment.EnchantmentDefinition definition, Set<String> slots) {
		Enchantment.EnchantmentDefinition def = new Enchantment.EnchantmentDefinition(definition.supportedItems(), definition.primaryItems(), definition.weight(), definition.maxLevel(),
				definition.minCost(), definition.maxCost(), definition.anvilCost(), definition.slots());

		((TrinketSlotTarget) (Object) def).trinkets$slots(slots);
		return def;
	}

	static {
		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "all"), (stack, ref, entity) -> TriState.TRUE);
		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "none"), (stack, ref, entity) -> TriState.FALSE);
		TagKey<Item> trinketsAll = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", "all"));

		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "tag"), (stack, ref, entity) -> {
			SlotType slot = ref.inventory().getSlotType();
			TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", slot.getId()));

			if (stack.is(tag) || stack.is(trinketsAll)) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "relevant"), (stack, ref, entity) -> {
			Multimap<Holder<Attribute>, AttributeModifier> map = TrinketModifiers.get(stack, ref, entity);
			if (!map.isEmpty()) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		DEFAULT_TRINKET = new Trinket() {

		};
	}
}