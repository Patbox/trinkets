package eu.pb4.trinkets.impl.slots;

import com.google.common.base.Predicates;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public interface TrinketSlotState {
    MutableObject<Constructor> CLIENT_CONSTRUCTOR = new MutableObject<>(MinimalTrinketSlotState::new);
    MutableObject<Constructor> SERVER_CONSTRUCTOR = new MutableObject<>(MinimalTrinketSlotState::new);

    static TrinketSlotState create(LivingEntity owner, AbstractContainerMenu inventoryMenu, LivingEntityTrinketAttachment trinkets, List<TrinketInventory> sortedInventories) {
        return (owner.level().isClientSide() ? CLIENT_CONSTRUCTOR : SERVER_CONSTRUCTOR).get().create(owner, inventoryMenu, trinkets, sortedInventories);
    }

    TrinketSlotState asCreativeState();

    @NotNull
    SlotInfo getSlotConfig(int slotIndex, TrinketInventory inventory, int index);

    record SlotInfo(int x, int y, boolean renderAfterRegularSlots, Predicate<TrinketSlot> isVisible) {
        public static final SlotInfo FALLBACK = new SlotInfo(-9999, -9999, false, Predicates.alwaysFalse());

        public SlotInfo reposition(int x, int y) {
            return new SlotInfo(x, y, renderAfterRegularSlots, isVisible);
        }
    }

    record Area2i(int x, int y, int width, int height) {
        public boolean contains(int x, int y) {
            return x >= this.x && y >= this.y && x < this.x + width && y < this.y + height;
        }
    }

    interface Constructor {
        TrinketSlotState create(LivingEntity owner, AbstractContainerMenu inventoryMenu, LivingEntityTrinketAttachment trinkets, List<TrinketInventory> sortedInventories);
    }
}
