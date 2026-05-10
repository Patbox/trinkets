package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableMap;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Map;

@ApiStatus.NonExtendable
public interface SlotGroup {
    /**
     * @return A sided map of slot group names to slot groups available for players
     */
    static Map<String, SlotGroup> getPlayerGroups(Level world) {
        return getEntityGroups(world, EntityType.PLAYER);
    }

    /**
     * @return A sided map of slot group names to slot groups available for players
     */
    static Map<String, SlotGroup> getPlayerGroups(Player player) {
        return getEntityGroups(player);
    }

    /**
     * @return A sided map of slot group names to slot groups available for the provided
     * entity type
     */
    static Map<String, SlotGroup> getEntityGroups(Level world, EntityType<?> type) {
        EntitySlotLoader loader = world.isClientSide() ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
        return loader.getEntityGroups(type);
    }

    /**
     * @return A sided map of slot group names to slot groups available for the provided
     * entity
     */
    static Map<String, SlotGroup> getEntityGroups(Entity entity) {
        if (entity != null) {
            return getEntityGroups(entity.level(), entity.getType());
        }
        return ImmutableMap.of();
    }

    @Deprecated(forRemoval = true)
    int slotId();

    boolean isAttachedToSlot(Slot slot);

    boolean hasSlotAttachment();

    int order();

    String name();

    Collection<SlotType> getSlots();

    /// Legacy slots by subId
    @Deprecated
    Map<String, SlotType> slots();
}
