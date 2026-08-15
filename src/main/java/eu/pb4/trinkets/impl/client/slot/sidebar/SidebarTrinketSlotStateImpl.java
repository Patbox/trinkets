package eu.pb4.trinkets.impl.client.slot.sidebar;

import com.google.common.base.Predicates;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.impl.*;
import eu.pb4.trinkets.impl.client.slot.ClientTrinketSlotState;
import eu.pb4.trinkets.impl.client.slot.TrinketScreenManagerBackend;
import eu.pb4.trinkets.impl.slots.TrinketSlotState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class SidebarTrinketSlotStateImpl implements TrinketSlotState, ClientTrinketSlotState {
    private final LivingEntity owner;
    private final Map<SlotType, List<SlotInfo>> slotInfo = new HashMap<>();
    private final AbstractContainerMenu menu;
    private final LivingEntityTrinketAttachment trinkets;
    private final int height;
    private final int width;
    private final int lastHeight;

    public SidebarTrinketSlotStateImpl(LivingEntity owner, AbstractContainerMenu menu, LivingEntityTrinketAttachment trinkets, List<TrinketInventory> sortedInventories) {
        this.owner = owner;
        this.menu = menu;
        this.trinkets = trinkets;

        this.height = TrinketsConfig.instance.sidebarHeight;

        int id = 0;
        for (var inventory : sortedInventories) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var x = 4 - (id / this.height * 18 + 18);
                var y = 8 + (id % this.height) * 18;

                id++;
                this.slotInfo.computeIfAbsent(inventory.slotType(), _ -> new ArrayList<>()).add(
                        new SlotInfo(x, y, false, Predicates.alwaysTrue())
                );
            }
        }

        this.width = id / TrinketsConfig.instance.sidebarHeight;
        this.lastHeight = id % TrinketsConfig.instance.sidebarHeight;
    }

    public int height() {
        return this.height;
    }

    public int lastHeight() {
        return this.lastHeight;
    }

    public int width() {
        return this.width;
    }

    @Override
    public TrinketSlotState asCreativeState() {
        return this;
    }

    @Override
    public @NonNull SlotInfo getSlotConfig(int slotIndex, TrinketInventory inventory, int index) {
        var list = this.slotInfo.get(inventory.slotType());
        if (list == null || list.size() <= index) {
            return SlotInfo.FALLBACK;
        }

        return list.get(index);
    }

    @Override
    public TrinketScreenManagerBackend getScreenBackend() {
        return SidebarTrinketScreenManager.INSTANCE;
    }
}
