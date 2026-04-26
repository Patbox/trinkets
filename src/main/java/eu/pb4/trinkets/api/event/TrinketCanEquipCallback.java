package eu.pb4.trinkets.api.event;

import dev.yumi.commons.TriState;
import dev.yumi.commons.event.Event;
import dev.yumi.mc.core.api.YumiEvents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketCanEquipCallback {
    Event<Identifier, TrinketCanEquipCallback> EVENT = YumiEvents.EVENTS.create(TrinketCanEquipCallback.class,
            listeners -> (stack, slot, entity, canEquipDefault) -> {
                for (var listener : listeners) {
                    var x = listener.canEquip(stack, slot, entity, canEquipDefault);
                    if (x != TriState.DEFAULT) {
                        return x;
                    }
                }

                return TriState.DEFAULT;
            });

    /**
     * Called when a trinket is checked for equipment, after the {@link eu.pb4.trinkets.api.callback.TrinketCallback#canEquip} method of the Trinket
     *
     * @param stack The stack being equipped
     * @param slot The slot the stack is equipped to
     * @param entity The entity that equipped the stack
     */
    TriState canEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, boolean canEquipDefault);
}