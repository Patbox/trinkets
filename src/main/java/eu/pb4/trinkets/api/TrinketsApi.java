package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableMap;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.event.TrinketDropCallback;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketSlotTarget;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.payload.BreakPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


public class TrinketsApi {
    /**
     * @return The trinket attachment for this entity
     */
    public static TrinketAttachment getAttachment(LivingEntity livingEntity) {
        return ((LivingEntityTrinketAttachment.Provider) livingEntity).trinkets$getAttachment();
    }

    /**
     * @return The active/final drop rule for select stack in a slot.
     */
    public static TrinketDropRule getDropRule(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, boolean keepInventory) {
        var dropRule = TrinketCallback.getCallback(stack).getDropRule(stack, slot, entity);
        dropRule = TrinketDropCallback.EVENT.invoker().drop(dropRule, stack, slot, entity);

        if (dropRule == TrinketDropRule.DEFAULT) {
            dropRule = slot.slotType().dropRule();
        }

        if (dropRule == TrinketDropRule.DEFAULT) {
            if (keepInventory && entity.getType() == EntityType.PLAYER) {
                dropRule = TrinketDropRule.KEEP;
            } else {
                if (EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                    dropRule = TrinketDropRule.DESTROY;
                } else {
                    dropRule = TrinketDropRule.DROP;
                }
            }
        }

        return dropRule;
    }

    /**
     * A simple to use, trinkets-aware implementation of {@link ItemStack#hurtAndBreak}
     */
    public void hurtAndBreakItemStack(ItemStack itemStack, int amount, LivingEntity owner, TrinketSlotAccess access) {
        if (owner.level() instanceof ServerLevel serverLevel) {
            itemStack.hurtAndBreak(amount, serverLevel, owner instanceof ServerPlayer player ? player : null, (brokenItem) -> onTrinketBroken(itemStack, access, owner));
        }
    }

    /**
     * Called to sync a trinket breaking event with clients. Should generally be
     * called in the callback of {@link ItemStack#hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer)}
     */
    public static void onTrinketBroken(ItemStack stack, TrinketSlotAccess ref, LivingEntity entity) {
        Level world = entity.level();
        if (!world.isClientSide()) {
            var packet = new ClientboundCustomPayloadPacket(new BreakPayload(entity.getId(), ref.inventory().slotType().group(), ref.inventory().slotType().name(), ref.index()));
            if (entity instanceof ServerPlayer player) {
                player.connection.send(packet);
            }

            if (entity.level().getChunkSource() instanceof ServerChunkCache cache) {
                cache.sendToTrackingPlayers(entity, packet);
            }
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

    /**
     * Modifies the EnchantmentDefinition to include trinkets slot support.
     */
    public static Enchantment.EnchantmentDefinition withTrinketSlots(Enchantment.EnchantmentDefinition definition, Set<String> slots) {
        Enchantment.EnchantmentDefinition def = new Enchantment.EnchantmentDefinition(definition.supportedItems(), definition.primaryItems(), definition.weight(), definition.maxLevel(),
                definition.minCost(), definition.maxCost(), definition.anvilCost(), definition.slots());

        ((TrinketSlotTarget) (Object) def).trinkets$slots(slots);
        return def;
    }
}