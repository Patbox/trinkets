package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableMap;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketSlotTarget;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.payload.BreakPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


public class TrinketsApi {
    /**
     * @return The trinket component for this entity, if available
     */
    public static TrinketAttachment getAttachment(LivingEntity livingEntity) {
        return ((LivingEntityTrinketAttachment.Provider) livingEntity).trinkets$getAttachment();
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
    public static void registerTrinketPredicate(Identifier id, TrinketPredicate predicate) {
        TrinketsMain.PREDICATES.put(id, predicate);
    }

    public interface TrinketPredicate {
        boolean test(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity);
    }

    public static Enchantment.EnchantmentDefinition withTrinketSlots(Enchantment.EnchantmentDefinition definition, Set<String> slots) {
        Enchantment.EnchantmentDefinition def = new Enchantment.EnchantmentDefinition(definition.supportedItems(), definition.primaryItems(), definition.weight(), definition.maxLevel(),
                definition.minCost(), definition.maxCost(), definition.anvilCost(), definition.slots());

        ((TrinketSlotTarget) (Object) def).trinkets$slots(slots);
        return def;
    }
}