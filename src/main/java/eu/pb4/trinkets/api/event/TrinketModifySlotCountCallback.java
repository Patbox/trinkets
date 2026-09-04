package eu.pb4.trinkets.api.event;

import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketInventory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public interface TrinketModifySlotCountCallback {
    Event<Identifier, TrinketModifySlotCountCallback> EVENT = YumiEvents.EVENTS.create(TrinketModifySlotCountCallback.class,
            listeners -> (entity, slotType, inventory, count) -> {
                for (var listener : listeners) {
                    count = listener.modifySlotCount(entity, slotType, inventory, count);
                }

                return count;
            });

    /**
     * Called when Trinkets updates inventory size, allowing you to manually update it.
     * Slot size calculation can be triggered via {@link TrinketInventory#updateSlotCount()}
     * This event only triggers on logical server!
     *
     * @param entity The entity that owns the inventory
     * @param slotType The type of the inventory
     * @param inventory Inventory itself
     * @param count currently selected inventory size.
     * @return Wanted size of the inventory. Return {@param count} to keep currently selected one.
     */
    int modifySlotCount(LivingEntity entity, SlotType slotType, TrinketInventory inventory, int count);
}