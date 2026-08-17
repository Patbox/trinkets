package eu.pb4.trinkets.api.event;

import dev.yumi.commons.TriState;
import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketCanUnequipCallback {
    Event<Identifier, TrinketCanUnequipCallback> EVENT = YumiEvents.EVENTS.create(TrinketCanUnequipCallback.class,
            listeners -> (stack, slot, entity, canUnequipDefault) -> {
                for (var listener : listeners) {
                    var x = listener.canUnequip(stack, slot, entity, canUnequipDefault);
                    if (x != TriState.DEFAULT) {
                        return x;
                    }
                }

                return TriState.DEFAULT;
            });

    /**
     * Called when a trinket is checked for being unequipped, after the {@link eu.pb4.trinkets.api.callback.TrinketCallback#canUnequip(ItemStack, TrinketSlotAccess, LivingEntity)} method of the Trinket
     *
     * @param stack The stack being equipped
     * @param slot The slot the stack is equipped to
     * @param entity The entity that equipped the stack
     */
    TriState canUnequip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, boolean canUnequipDefault);
}