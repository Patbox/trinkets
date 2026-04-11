package eu.pb4.trinkets.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public final class TrinketInventoryImpl implements TrinketInventory {
    private final SlotType slotType;
    private final int baseSize;
    private final TrinketAttachment attachment;
    private final Map<Identifier, AttributeModifier> modifiers = new HashMap<>();
    private final Set<AttributeModifier> persistentModifiers = new HashSet<>();
    private final Set<AttributeModifier> cachedModifiers = new HashSet<>();
    private final Multimap<AttributeModifier.Operation, AttributeModifier> modifiersByOperation = HashMultimap.create();
    private final InventorySizeChangedCallback updateSizeCallback;
    private final Consumer<TrinketInventoryImpl> markDirty;
    private TrinketSlotAccess[] accesses;
    private NonNullList<ItemStack> stacks;
    private int size;
    private boolean update = false;
    private int forcedSlotCount;
    boolean isValid = true;

    public TrinketInventoryImpl(SlotType slotType, TrinketAttachment comp, Consumer<TrinketInventoryImpl> markDirty, InventorySizeChangedCallback updateSizeCallback, boolean clientSide) {
        this.attachment = comp;
        this.slotType = slotType;
        this.baseSize = slotType.amount();
        this.stacks = NonNullList.withSize(this.baseSize, ItemStack.EMPTY);
        this.updateSlotAccess();
        this.size = this.baseSize;
        this.forcedSlotCount = clientSide ? this.baseSize : -1;
        this.updateSizeCallback = updateSizeCallback;
        this.markDirty = markDirty;
    }

    private void updateSlotAccess() {
        int index;
        if (this.accesses == null) {
            this.accesses = new TrinketSlotAccess[this.stacks.size()];
            index = 0;
        } else {
            index = this.accesses.length;
            this.accesses = Arrays.copyOf(this.accesses, this.stacks.size());
        }

        for (; index < this.accesses.length; index++) {
            this.accesses[index] = new TrinketSlotAccess(this, index);
        }
    }

    public static void copyFrom(LivingEntity previous, LivingEntity current) {
        var prevTrinkets = LivingEntityTrinketAttachment.get(previous);
        var currentTrinkets = LivingEntityTrinketAttachment.get(current);

        var prevMap = prevTrinkets.getInventory();
        var currentMap = currentTrinkets.getInventory();
        for (var entry : prevMap.entrySet()) {
            var currentInvs = currentMap.get(entry.getKey());
            if (currentInvs != null) {
                for (var invEntry : entry.getValue().entrySet()) {
                    var currentInv = currentInvs.get(invEntry.getKey());
                    if (currentInv != null) {
                        currentInv.copyFrom(invEntry.getValue());
                    }
                }
            }
        }
    }

    public SlotType slotType() {
        return this.slotType;
    }

    @Override
    public @Nullable TrinketSlotAccess getSlotAccess(int slot) {
        return this.isValidSlot(slot) ? this.accesses[slot] : null;
    }

    @Override
    public boolean isValidSlot(int index) {
        return index < this.accesses.length && this.isValid;
    }


    @Override
    public TrinketAttachment getAttachment() {
        return this.attachment;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        this.update();
        return this.stacks.size();
    }

    public int getSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!stacks.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        this.update();
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(stacks, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(stacks, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.update();
        stacks.set(slot, stack);
    }

    @Override
    public void setChanged() {
        this.markDirty.accept(this);
    }

    public void markUpdate() {
        this.update = true;
        this.markDirty.accept(this);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isValid;
    }

    public Map<Identifier, AttributeModifier> getModifiers() {
        return this.modifiers;
    }

    public Collection<AttributeModifier> getModifiersByOperation(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.get(operation);
    }

    public void addModifier(AttributeModifier modifier) {
        this.modifiers.put(modifier.id(), modifier);
        this.getModifiersByOperation(modifier.operation()).add(modifier);
        this.markUpdate();
    }

    public void addModifiers(AttributeModifier modifier) {
        this.addModifier(modifier);
        this.persistentModifiers.add(modifier);
    }

    public void removeModifier(Identifier identifier) {
        AttributeModifier modifier = this.modifiers.remove(identifier);
        if (modifier != null) {
            this.persistentModifiers.remove(modifier);
            this.getModifiersByOperation(modifier.operation()).remove(modifier);
            this.markUpdate();
        }
    }

    public void clearModifiers() {
        java.util.Iterator<Identifier> iter = this.getModifiers().keySet().iterator();

        while (iter.hasNext()) {
            this.removeModifier(iter.next());
        }
    }

    public void removeCachedModifier(AttributeModifier attributeModifier) {
        this.cachedModifiers.remove(attributeModifier);
    }

    public void clearCachedModifiers() {
        for (AttributeModifier cachedModifier : this.cachedModifiers) {
            this.removeModifier(cachedModifier.id());
        }
        this.cachedModifiers.clear();
    }

    public void setSlotCount(int value) {
        this.forcedSlotCount = value;
        this.markUpdate();
        this.update();
    }

    public void update() {
        if (this.update) {
            this.update = false;
            if (this.forcedSlotCount < 0) {
                double baseSize = this.baseSize;
                for (AttributeModifier mod : this.getModifiersByOperation(AttributeModifier.Operation.ADD_VALUE)) {
                    baseSize += mod.amount();
                }

                double size = baseSize;
                for (AttributeModifier mod : this.getModifiersByOperation(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
                    size += this.baseSize * mod.amount();
                }

                for (AttributeModifier mod : this.getModifiersByOperation(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
                    size *= mod.amount();
                }

                this.size = (int) size;
            } else {
                this.size = this.forcedSlotCount;
            }

            LivingEntity entity = this.attachment.getEntity();

            if (this.size != this.stacks.size()) {
                var oldSize = this.stacks.size();
                NonNullList<ItemStack> newStacks = NonNullList.withSize(this.size, ItemStack.EMPTY);
                for (int i = 0; i < this.stacks.size(); i++) {
                    ItemStack stack = this.stacks.get(i);
                    if (i < newStacks.size()) {
                        newStacks.set(i, stack);
                    } else {
                        if (entity.level() instanceof ServerLevel serverWorld) {
                            entity.spawnAtLocation(serverWorld, stack);
                        }
                    }
                }

                this.stacks = newStacks;
                this.updateSlotAccess();
                this.updateSizeCallback.callSizeChanged(this, oldSize, this.size);
            }
        }
    }

    @Override
    public void copyFrom(TrinketInventory value) {
        this.copyFrom((TrinketInventoryImpl) value);
    }

    public void copyFrom(TrinketInventoryImpl other) {
        this.modifiers.clear();
        this.modifiersByOperation.clear();
        this.persistentModifiers.clear();
        other.modifiers.forEach((uuid, modifier) -> this.addModifier(modifier));
        for (AttributeModifier persistentModifier : other.persistentModifiers) {
            this.addModifiers(persistentModifier);
        }
        this.update = true;
        this.update();
    }

    public TrinketSaveData.Metadata toMetadata() {
        List<AttributeModifier> cachedModifiers = new ArrayList<>();

        if (!this.modifiers.isEmpty()) {
            this.modifiers.forEach((uuid, modifier) -> {
                if (!this.persistentModifiers.contains(modifier)) {
                    cachedModifiers.add(modifier);
                }
            });
        }
        return new TrinketSaveData.Metadata(List.copyOf(this.persistentModifiers), cachedModifiers);
    }

    public void fromMetadata(TrinketSaveData.Metadata tag, int size) {
        this.size = size;
        tag.persistentModifiers().forEach(this::addModifiers);

        if (!tag.cachedModifiers().isEmpty()) {
            for (AttributeModifier modifier : tag.cachedModifiers()) {
                this.cachedModifiers.add(modifier);
                this.addModifier(modifier);
            }

            this.update();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrinketInventoryImpl that = (TrinketInventoryImpl) o;
        return slotType.equals(that.slotType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotType);
    }

    public interface InventorySizeChangedCallback {
        void callSizeChanged(TrinketInventoryImpl inventory, int oldCount, int newCount);
    }
}