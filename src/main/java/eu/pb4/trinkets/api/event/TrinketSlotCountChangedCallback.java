package eu.pb4.trinkets.api.event;

import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketInventory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public interface TrinketSlotCountChangedCallback {
    Event<Identifier, TrinketSlotCountChangedCallback> EVENT = YumiEvents.EVENTS.create(TrinketSlotCountChangedCallback.class,
            listeners -> (entity, slotType, inventory, oldCount, newCount) -> {
                for (var listener : listeners) {
                    listener.onTrinketSlotCountChanged(entity, slotType, inventory, oldCount, newCount);
                }
            });

    /**
     * Called after Trinkets updates it's inventory size.
     * This event only triggers on client and  server!
     *
     * @param entity The entity that owns the inventory
     * @param slotType The type of the inventory
     * @param inventory Inventory itself
     * @param oldCount Previous amount of slots
     * @param newCount New amount of slots
     */
    void onTrinketSlotCountChanged(LivingEntity entity, SlotType slotType, TrinketInventory inventory, int oldCount, int newCount);
}