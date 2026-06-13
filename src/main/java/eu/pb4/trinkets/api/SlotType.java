package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableMap;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.NonExtendable
public interface SlotType {
    /**
     * @return A sided map of slot ids to slots available for players
     */
    static Map<String, SlotType> getPlayerSlots(Level world) {
        return getEntitySlots(world, EntityTypes.PLAYER);
    }

    /**
     * @return A sided map of slot ids to slots available for players
     */
    static Map<String, SlotType> getPlayerSlots(Player player) {
        return getEntitySlots(player);
    }

    /**
     * @return A sided map of slot ids to slots available for the provided
     * entity type
     */
    static Map<String, SlotType> getEntitySlots(Level world, EntityType<?> type) {
        EntitySlotLoader loader = world.isClientSide() ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
        return loader.getEntitySlotTypes(type);
    }

    /**
     * @return A sided map of slot ids to slots available for the provided
     * entity
     */
    static Map<String, SlotType> getEntitySlots(Entity entity) {
        if (entity != null) {
            return getEntitySlots(entity.level(), entity.getType());
        }
        return ImmutableMap.of();
    }

    /**
     * @return A sided map of slot ids to all existing slot types
     */
    static Map<String, SlotType> getAllSlots(Level world) {
        EntitySlotLoader loader = world.isClientSide() ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
        return loader.getSlotTypes();
    }

    MutableComponent getTranslation();

    String getId();

    String group();

    int order();

    int amount();

    Identifier icon();

    boolean quickMoveCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);

    boolean interactEquipableCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);

    boolean validatorCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);

    boolean tooltipCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);

    TrinketDropRule dropRule();

    boolean isVanityOnly();

    boolean isHidden();

    int maxStackSize(ItemStack stack);

    int maxStackSize();

    @Deprecated
    String name();
}
