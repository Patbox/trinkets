package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Function3;
import eu.pb4.trinkets.impl.TrinketSlotTarget;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.payload.BreakPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static eu.pb4.trinkets.impl.LivingEntityTrinketComponent.TRINKET_COMPONENT;

public class TrinketsApi {
    private static final Map<Identifier, Function3<ItemStack, TrinketSlotAccess, LivingEntity, TriState>> PREDICATES = new HashMap<>();

    /**
     * @return The trinket component for this entity, if available
     */
    public static Optional<TrinketAttachment> getTrinketAttachment(LivingEntity livingEntity) {
        return TRINKET_COMPONENT.maybeGet(livingEntity).map(Function.identity());
    }

    /**
     * Called to sync a trinket breaking event with clients. Should generally be
     * called in the callback of {@link ItemStack#hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer)}
     */
    public static void onTrinketBroken(ItemStack stack, TrinketSlotAccess ref, LivingEntity entity) {
        Level world = entity.level();
        if (!world.isClientSide()) {
            BreakPayload packet = new BreakPayload(entity.getId(), ref.inventory().slotType().group(), ref.inventory().slotType().name(), ref.index());
            if (entity instanceof ServerPlayer player) {
                ServerPlayNetworking.send(player, packet);
            }
            PlayerLookup.tracking(entity).forEach(watcher -> {
                ServerPlayNetworking.send(watcher, packet);
            });
        }
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
    public static void registerTrinketPredicate(Identifier id, Function3<ItemStack, TrinketSlotAccess, LivingEntity, TriState> predicate) {
        PREDICATES.put(id, predicate);
    }

    public static Optional<Function3<ItemStack, TrinketSlotAccess, LivingEntity, TriState>> getTrinketPredicate(Identifier id) {
        return Optional.ofNullable(PREDICATES.get(id));
    }

    public static boolean evaluatePredicateSet(Set<Identifier> set, ItemStack stack, TrinketSlotAccess ref, LivingEntity entity) {
        TriState state = TriState.DEFAULT;
        for (Identifier id : set) {
            Optional<Function3<ItemStack, TrinketSlotAccess, LivingEntity, TriState>> function = getTrinketPredicate(id);
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
}