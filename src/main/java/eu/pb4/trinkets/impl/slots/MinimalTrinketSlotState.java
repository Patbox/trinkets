package eu.pb4.trinkets.impl.slots;

import com.google.common.base.Predicates;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class MinimalTrinketSlotState implements TrinketSlotState {
    public MinimalTrinketSlotState(LivingEntity entity, AbstractContainerMenu menu, LivingEntityTrinketAttachment livingEntityTrinketAttachment, List<TrinketInventory> trinketInventories) {
    }

    @Override
    public TrinketSlotState asCreativeState() {
        return this;
    }

    @Override
    public @NonNull SlotInfo getSlotConfig(int slotIndex, TrinketInventory inventory, int index) {
        return new SlotInfo(-(slotIndex % 5) * 18, slotIndex / 5  * 18, false, Predicates.alwaysTrue());
    }
}
