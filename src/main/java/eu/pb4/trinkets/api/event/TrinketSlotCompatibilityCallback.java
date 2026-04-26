package eu.pb4.trinkets.api.event;

import dev.yumi.commons.TriState;
import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketSlotCompatibilityCallback {
    Event<Identifier, TrinketSlotCompatibilityCallback> EVENT = YumiEvents.EVENTS.create(TrinketSlotCompatibilityCallback.class,
            listeners -> (stack, slot, entity, canEquipDefault) -> {
                for (var listener : listeners) {
                    var x = listener.isTrinketSlotCompatible(stack, slot, entity, canEquipDefault);
                    if (x != TriState.DEFAULT) {
                        return x;
                    }
                }

                return TriState.DEFAULT;
            });

    /**
     * Called when trinket is checked against being compatible with a select slot type.
     *
     * @param stack The stack being equipped
     * @param slot The slot the stack is equipped to
     * @param entity The entity that equipped the stack
     */
    TriState isTrinketSlotCompatible(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, boolean canEquipDefault);
}