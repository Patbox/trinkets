package eu.pb4.trinkets.impl.client.slot.legacy;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.impl.*;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import eu.pb4.trinkets.impl.client.slot.ClientTrinketSlotState;
import eu.pb4.trinkets.impl.client.slot.TrinketScreenManagerBackend;
import eu.pb4.trinkets.impl.slots.SurvivalTrinketSlot;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import eu.pb4.trinkets.impl.slots.TrinketSlotState;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class LegacyTrinketSlotStateImpl implements LegacyTrinketSlotState, ClientTrinketSlotState {
    private final Map<SlotGroup, Integer> groupNums = new HashMap<>();
    private final Map<SlotGroup, Point> groupPos = new HashMap<>();
    private final Int2ObjectMap<SlotGroup> slotToGroup = new Int2ObjectOpenHashMap<>();
    private final Map<SlotGroup, List<Point>> slotHeights = new HashMap<>();
    private final Map<SlotGroup, List<SlotType>> slotTypes = new HashMap<>();
    private final Map<SlotGroup, Integer> slotWidths = new HashMap<>();
    private final LivingEntity owner;

    private final Map<SlotType, List<SlotInfo>> slotInfo = new HashMap<>();
    private final boolean forceSidebar;

    private int groupCount = 0;

    private final AbstractContainerMenu menu;
    private final LivingEntityTrinketAttachment trinkets;

    public LegacyTrinketSlotStateImpl(LivingEntity owner, AbstractContainerMenu menu, LivingEntityTrinketAttachment trinkets, List<TrinketInventory> sortedInventories, boolean forceSidebar) {
        this.forceSidebar = forceSidebar;
        this.owner = owner;
        this.menu = menu;
        this.trinkets = trinkets;

        Map<String, SlotGroup> groups = trinkets.getGroups();
        groupPos.clear();
        slotToGroup.clear();

        int maxHeight = TrinketsConfig.instance.sidebarHeight;
        int groupNum = this.forceSidebar ? 4 : 1; // Start at 1 because offhand exists

        for (SlotGroup group : groups.values().stream().sorted(Comparator.comparing(SlotGroup::order).thenComparing(SlotGroup::name)).toList()) {
            if (!hasSlots(trinkets, group)) {
                continue;
            }

            if (group.hasSlotAttachment() && !this.forceSidebar) {
                int id = group.slotId();

                if (this.menu.slots.size() > id) {
                    Slot slot = this.menu.slots.get(id);
                    if (!(slot instanceof SurvivalTrinketSlot)) {
                        groupPos.put(group, new Point(slot.x, slot.y));
                        slotToGroup.put(id, group);
                        groupNums.put(group, -id);
                    }
                }
            } else {
                int x = 77;
                int y;
                if (groupNum >= 4) {
                    x = 4 - ((groupNum - 4) / maxHeight) * 18 - 18;
                    y = 8 + ((groupNum - 4) % maxHeight) * 18;
                } else {
                    y = 62 - groupNum * 18;
                }
                groupPos.put(group, new Point(x, y));
                groupNums.put(group, groupNum);
                groupNum++;
            }
        }
        groupCount = Math.max(0, groupNum - 4);
        slotWidths.clear();
        slotHeights.clear();
        slotTypes.clear();
        for (var entry : trinkets.getLegacyInventoryImpl().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            String groupId = entry.getKey();
            SlotGroup group = groups.get(groupId);
            int groupOffset = 1;

            if (group.hasSlotAttachment() && !this.forceSidebar) {
                groupOffset++;
            }
            int width = 0;
            Point pos = getGroupPos(group);
            if (pos == null) {
                continue;
            }
            for (var slot : entry.getValue().entrySet().stream().sorted(
                    Map.Entry.comparingByValue(Comparator.<TrinketInventoryImpl>comparingInt(x -> x.slotType().order())
                            .thenComparing(x -> x.slotType().getId()))
            ).toList()) {
                var stacks = slot.getValue();
                if (stacks.getContainerSize() == 0 || slot.getValue().slotType().isHidden()) {
                    continue;
                }
                int slotOffset = 1;
                int x = (int) ((groupOffset / 2) * 18 * Math.pow(-1, groupOffset));
                slotHeights.computeIfAbsent(group, (k) -> new ArrayList<>()).add(new Point(x, stacks.getContainerSize()));
                slotTypes.computeIfAbsent(group, (k) -> new ArrayList<>()).add(stacks.slotType());

                for (int i = 0; i < stacks.getContainerSize(); i++) {
                    int y = (int) (pos.y() + (slotOffset / 2) * 18 * Math.pow(-1, slotOffset));

                    this.slotInfo.computeIfAbsent(slot.getValue().slotType(), _ -> new ArrayList<>()).add(
                            new SlotInfo(x + pos.x(), y, i != 0 || groupOffset != 1, groupOffset == 1 && i == 0 ? Predicates.alwaysTrue() : this::isSlotVisible)
                    );
                    slotOffset++;
                }
                groupOffset++;
                width++;
            }
            slotWidths.put(group, width);
        }
    }

    private boolean isSlotVisible(TrinketSlot trinketSlot) {
        if (TrinketsMain.IS_CLIENT) {
            if (TrinketsClient.activeGroup == trinketSlot.getGroup()) {
                return trinketSlot.asSlot().getContainerSlot() == 0 || TrinketsClient.activeType == trinketSlot.getType();
            } else if (TrinketsClient.quickMoveGroup == trinketSlot.getGroup()) {
                return trinketSlot.asSlot().getContainerSlot() == 0 || TrinketsClient.quickMoveType == trinketSlot.getType() && TrinketsClient.quickMoveTimer > 0;
            }

            return false;
        }

        return true;
    }

    private boolean hasSlots(TrinketAttachment comp, SlotGroup group) {
        for (var inv : comp.getInventory().get(group.name()).values()) {
            if (inv.getContainerSize() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getGroupNum(SlotGroup group) {
        return groupNums.getOrDefault(group, 0);
    }

    @Override
    public @Nullable Point getGroupPos(SlotGroup group) {
        return groupPos.get(group);
    }

    @Override
    public @Nullable SlotGroup getGroupAtSlot(int slotIndex) {
        return slotToGroup.get(slotIndex);
    }

    @Override
    public @NotNull List<Point> getSlotHeights(SlotGroup group) {
        return slotHeights.getOrDefault(group, ImmutableList.of());
    }

    @Override
    public @Nullable Point getSlotHeight(SlotGroup group, int i) {
        List<Point> points = this.getSlotHeights(group);
        return i < points.size() ? points.get(i) : null;
    }

    @Override
    public @NotNull List<SlotType> getSlotTypes(SlotGroup group) {
        return slotTypes.getOrDefault(group, ImmutableList.of());
    }

    public int getSlotWidth(SlotGroup group) {
        return slotWidths.getOrDefault(group, 0);
    }

    @Override
    public int groupCount() {
        return groupCount;
    }

    @Override
    public LegacyTrinketSlotState asCreativeState() {
        return new Creative();
    }

    @Override
    public boolean forceSidebar() {
        return this.forceSidebar;
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
        return LegacyTrinketScreenManager.INSTANCE;
    }

    public static TrinketSlotState classic(LivingEntity livingEntity, AbstractContainerMenu menu, LivingEntityTrinketAttachment livingEntityTrinketAttachment, List<TrinketInventory> trinketInventories) {
        return new LegacyTrinketSlotStateImpl(livingEntity, menu, livingEntityTrinketAttachment, trinketInventories, false);
    }

    public static TrinketSlotState sidebar(LivingEntity livingEntity, AbstractContainerMenu menu, LivingEntityTrinketAttachment livingEntityTrinketAttachment, List<TrinketInventory> trinketInventories) {
        return new LegacyTrinketSlotStateImpl(livingEntity, menu, livingEntityTrinketAttachment, trinketInventories, true);
    }

    private class Creative implements LegacyTrinketSlotState, ClientTrinketSlotState {
        @Override
        public int getGroupNum(SlotGroup group) {
            return LegacyTrinketSlotStateImpl.this.getGroupNum(group);
        }

        @Override
        public @Nullable Point getGroupPos(SlotGroup group) {
            int groupNum = LegacyTrinketSlotStateImpl.this.getGroupNum(group);
            if (groupNum <= 3) {
                // Look what else do you want me to do
                return switch (groupNum) {
                    case 1 -> new Point(16, 20);
                    case 2 -> new Point(127, 20);
                    case 3 -> new Point(146, 20);
                    case -5 -> new Point(54, 6);
                    case -6 -> new Point(54, 33);
                    case -7 -> new Point(108, 6);
                    case -8 -> new Point(108, 33);
                    case -45 -> new Point(35, 20);
                    default -> null;
                };
            }
            
            return LegacyTrinketSlotStateImpl.this.getGroupPos(group);
        }

        @Override
        public @Nullable SlotGroup getGroupAtSlot(int slotIndex) {
            return LegacyTrinketSlotStateImpl.this.getGroupAtSlot(slotIndex);
        }

        @Override
        public @NotNull List<Point> getSlotHeights(SlotGroup group) {
            return LegacyTrinketSlotStateImpl.this.getSlotHeights(group);
        }

        @Override
        public @Nullable Point getSlotHeight(SlotGroup group, int i) {
            return LegacyTrinketSlotStateImpl.this.getSlotHeight(group, i);
        }

        @Override
        public @NotNull List<SlotType> getSlotTypes(SlotGroup group) {
            return LegacyTrinketSlotStateImpl.this.getSlotTypes(group);
        }

        @Override
        public int getSlotWidth(SlotGroup group) {
            return LegacyTrinketSlotStateImpl.this.getSlotWidth(group);
        }

        @Override
        public int groupCount() {
            return LegacyTrinketSlotStateImpl.this.groupCount();
        }

        @Override
        public LegacyTrinketSlotState asCreativeState() {
            return this;
        }

        @Override
        public boolean forceSidebar() {
            return LegacyTrinketSlotStateImpl.this.forceSidebar();
        }

        @Override
        public @NonNull SlotInfo getSlotConfig(int slotIndex, TrinketInventory inventory, int index) {
            var info = LegacyTrinketSlotStateImpl.this.getSlotConfig(slotIndex, inventory, index);
            if (info == null) {
                return SlotInfo.FALLBACK;
            }
            var group = SlotGroup.getEntityGroups(owner).get(inventory.slotType().group());
            var posA = LegacyTrinketSlotStateImpl.this.getGroupPos(group);
            var posB = this.getGroupPos(group);

            return info.reposition(info.x() + posB.x() - posA.x(), info.y() + posB.y() - posA.y());
        }

        @Override
        public TrinketScreenManagerBackend getScreenBackend() {
            return LegacyTrinketScreenManager.INSTANCE;
        }
    }
}
