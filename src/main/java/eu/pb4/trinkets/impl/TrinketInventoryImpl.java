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
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    private final boolean clientSide;
    private TrinketSlotAccess[] accesses;
    private TrinketSlotAccess[] cosmeticAccess;
    NonNullList<ItemStack> stacks;
    NonNullList<ItemStack> cosmeticStacks;
    private int size;
    private boolean update = false;
    private boolean suppressUpdates = false;
    private int forcedSlotCount;
    public boolean isVisibilityDirty = false;
    boolean isValid = true;
    public BitSet hiddenSlots;

    public TrinketInventoryImpl(SlotType slotType, TrinketAttachment comp, Consumer<TrinketInventoryImpl> markDirty, InventorySizeChangedCallback updateSizeCallback, boolean clientSide) {
        this.attachment = comp;
        this.slotType = slotType;
        this.baseSize = slotType.amount();
        this.stacks = NonNullList.withSize(this.baseSize, ItemStack.EMPTY);
        this.cosmeticStacks = cosmeticItemsEnabled() ? NonNullList.withSize(this.baseSize, ItemStack.EMPTY) : NonNullList.createWithCapacity(0);
        this.hiddenSlots = new BitSet(this.baseSize);
        this.updateSlotAccess();
        this.size = this.baseSize;
        this.forcedSlotCount = clientSide ? this.baseSize : -1;
        this.updateSizeCallback = updateSizeCallback;
        this.markDirty = markDirty;
        this.clientSide = clientSide;
    }

    private void updateSlotAccess() {
        {
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

        if (this.cosmeticItemsEnabled()) {
            int index;
            if (this.cosmeticAccess == null) {
                this.cosmeticAccess = new TrinketSlotAccess[this.stacks.size()];
                index = 0;
            } else {
                index = this.cosmeticAccess.length;
                this.cosmeticAccess = Arrays.copyOf(this.cosmeticAccess, this.stacks.size());
            }

            for (; index < this.cosmeticAccess.length; index++) {
                this.cosmeticAccess[index] = new TrinketSlotAccess(this, index, true);
            }
        } else {
            this.cosmeticAccess = new TrinketSlotAccess[]{};
        }
    }

    public static void copyFrom(LivingEntity previous, LivingEntity current) {
        var prevTrinkets = LivingEntityTrinketAttachment.get(previous);
        var currentTrinkets = LivingEntityTrinketAttachment.get(current);

        var prevMap = prevTrinkets.inventory;
        var currentMap = currentTrinkets.inventory;
        for (var entry : prevMap.entrySet()) {
            var currentInv = currentMap.get(entry.getKey());
            if (currentInv != null) {
                currentInv.copyFrom(entry.getValue());
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
    public TrinketSlotAccess getOrCreateSlotAccess(int slot) {
        return slot < this.accesses.length ? this.accesses[slot] : new TrinketSlotAccess(this, slot);
    }

    @Override
    public @Nullable TrinketSlotAccess getCosmeticSlotAccess(int slot) {
        return this.isValidSlot(slot) && this.hasCosmeticItems() ? this.cosmeticAccess[slot] : null;
    }

    @Override
    public TrinketSlotAccess getOrCreateCosmeticSlotAccess(int slot) {
        return slot < this.cosmeticAccess.length ? this.cosmeticAccess[slot] : new TrinketSlotAccess(this, slot);
    }

    @Override
    public ItemStack getCosmeticItem(int slot) {
        return this.hasCosmeticItems() ? this.cosmeticStacks.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public boolean setCosmeticItem(int slot, ItemStack itemStack) {
        if (this.hasCosmeticItems()) {
            this.cosmeticStacks.set(slot, itemStack);
            return true;
        }
        return false;
    }

    public boolean cosmeticItemsEnabled() {
        return TrinketsConfig.getGameplay(this.clientSide).cosmeticSlots && this.slotType.supportsCosmeticSlots();
    }

    @Override
    public boolean hasCosmeticItems() {
        return cosmeticItemsEnabled() && !this.cosmeticStacks.isEmpty() && this.cosmeticAccess != null && this.cosmeticAccess.length > 0;
    }

    @Override
    public boolean isValidSlot(int index) {
        return this.isValid && this.isLegalSlot(index);
    }

    private boolean isLegalSlot(int index) {
        return index < this.accesses.length && index >= 0;
    }

    @Override
    public TrinketAttachment getAttachment() {
        return this.attachment;
    }

    @Override
    public boolean isVisible(int index) {
        return !TrinketsConfig.getGameplay(this.clientSide).equipmentHiding || !this.hiddenSlots.get(index);
    }

    public void setVisible(int index, boolean value) {
        if (index >= this.size) {
            return;
        }

        this.hiddenSlots.set(index, !value);
        this.isVisibilityDirty = true;
        this.markUpdate();
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

    public boolean isCosmeticEmpty() {
        for (int i = 0; i < this.cosmeticStacks.size(); i++) {
            if (!this.cosmeticStacks.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        this.update();
        return this.isLegalSlot(slot) ? stacks.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.isLegalSlot(slot) ? ContainerHelper.removeItem(stacks, slot, amount) : ItemStack.EMPTY;
    }

    public ItemStack removeCosmeticItem(int slot, int amount) {
        return this.isLegalSlot(slot) ? ContainerHelper.removeItem(cosmeticStacks, slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.isLegalSlot(slot) ? ContainerHelper.takeItem(stacks, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.update();
        if (this.isLegalSlot(slot)) {
            stacks.set(slot, stack);
        }
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

    @Override
    public int getMaxStackSize() {
        return this.slotType.maxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return this.slotType.maxStackSize(itemStack);
    }

    public Map<Identifier, AttributeModifier> getModifiers() {
        return this.modifiers;
    }

    public Collection<AttributeModifier> getModifiersByOperation(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.get(operation);
    }

    public void addModifierInternal(AttributeModifier modifier) {
        this.modifiers.put(modifier.id(), modifier);
        this.getModifiersByOperation(modifier.operation()).add(modifier);
        this.markUpdate();
    }

    // Persistent
    public void addModifiers(AttributeModifier modifier) {
        this.addModifierInternal(modifier);
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
        if (this.update && !suppressUpdates) {
            this.update = false;
            this.suppressUpdates = true;
            if (this.forcedSlotCount < 0) {
                this.size = this.calculateNewSize();
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
                        TrinketSlotAccess ref = this.getSlotAccess(i);
                        if (ref == null) {
                            continue;
                        }
                        ItemStack oldStack = stack;
                        if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory) {
                            oldStack = stackHistory.trinkets$getOldStack(ref);
                        }
                        TrinketUtilities.callTrinketEquipmentChange(oldStack, ItemStack.EMPTY, ref, entity);
                        if (this.getAttachment() instanceof LivingEntityTrinketAttachment livingEntityTrinketAttachment) {
                            livingEntityTrinketAttachment.stopTrinketLocationBasedEffects(oldStack, ref, entity.getAttributes());
                        }
                        if (entity.level() instanceof ServerLevel serverWorld) {
                            entity.spawnAtLocation(serverWorld, stack);
                        }
                        this.stacks.set(i, ItemStack.EMPTY);
                        if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory && !stackHistory.trinkets$getOldStack(ref).isEmpty()) {
                            stackHistory.trinkets$resolveOldStack(ref);
                        }
                    }
                }

                this.stacks = newStacks;
                var old = this.hiddenSlots;
                this.hiddenSlots = new BitSet(size);

                for (int i = 0; i < Math.min(size, old.length()); i++) {
                    this.hiddenSlots.set(i, old.get(i));
                }

                this.updateCosmeticSlots();

                this.updateSlotAccess();
                this.updateSizeCallback.callSizeChanged(this, oldSize, this.size);
            } else if (this.cosmeticStacks.size() != this.stacks.size()) {
                this.updateCosmeticSlots();
                this.updateSlotAccess();
            }


            // Process updates sequentially, instead of in the middle of an incomplete update.
            this.suppressUpdates = false;
            this.update();
        }
    }

    private void updateCosmeticSlots() {
        LivingEntity entity = this.attachment.getEntity();

        if (this.cosmeticItemsEnabled()) {
            NonNullList<ItemStack> newStacksDeco = NonNullList.withSize(this.size, ItemStack.EMPTY);
            for (int i = 0; i < this.cosmeticStacks.size(); i++) {
                ItemStack stack = this.cosmeticStacks.get(i);
                if (i < newStacksDeco.size()) {
                    newStacksDeco.set(i, stack);
                } else {
                    TrinketSlotAccess ref = this.getOrCreateCosmeticSlotAccess(i);
                    if (ref == null) {
                        continue;
                    }
                    ItemStack oldStack = stack;
                    if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory) {
                        oldStack = stackHistory.trinkets$getOldStack(ref);
                    }
                    TrinketUtilities.callTrinketEquipmentChange(oldStack, ItemStack.EMPTY, ref, entity);

                    if (entity.level() instanceof ServerLevel serverWorld) {
                        entity.spawnAtLocation(serverWorld, stack);
                    }
                    this.cosmeticStacks.set(i, ItemStack.EMPTY);
                    if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory && !stackHistory.trinkets$getOldStack(ref).isEmpty()) {
                        stackHistory.trinkets$resolveOldStack(ref);
                    }
                }
            }
            this.cosmeticStacks = newStacksDeco;
        } else {
            if (entity.level() instanceof ServerLevel serverWorld) {
                for (var stack : this.cosmeticStacks) {
                    entity.spawnAtLocation(serverWorld, stack);
                }
            }

            this.cosmeticStacks = NonNullList.createWithCapacity(0);
        }
    }

    private int calculateNewSize() {
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

        return (int) size;
    }

    @SuppressWarnings("removal")
    @Override
    public void copyFrom(TrinketInventory value) {
        if (value instanceof TrinketInventoryImpl other) {
            this.copyFrom(other);
        }
    }

    public void copyFrom(TrinketInventoryImpl other) {
        this.modifiers.clear();
        this.modifiersByOperation.clear();
        this.persistentModifiers.clear();
        other.modifiers.forEach((uuid, modifier) -> this.addModifierInternal(modifier));
        for (AttributeModifier persistentModifier : other.persistentModifiers) {
            this.addModifiers(persistentModifier);
        }
        this.forcedSlotCount = other.forcedSlotCount;
        this.update = true;
        this.update();
        for (int i = 0; i < this.size; i++) {
            this.hiddenSlots.set(i, other.hiddenSlots.get(i));
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

    public boolean skipSaving() {
        return this.isEmpty() && this.isCosmeticEmpty() && this.cachedModifiers.isEmpty() && this.modifiers.isEmpty();
    }

    public void writeData(ValueOutput value) {
        ContainerSavingHelper.saveAllItems(value, this.stacks);
        ContainerSavingHelper.saveAllItems("cosmetic", value, this.cosmeticStacks);

        if (!this.persistentModifiers.isEmpty()) {
            var list = value.list("persistent_modifiers", AttributeModifier.CODEC);
            this.persistentModifiers.forEach(list::add);
        }
        if (!this.cachedModifiers.isEmpty()) {
            var list = value.list("cached_modifiers", AttributeModifier.CODEC);
            this.cachedModifiers.forEach(list::add);
        }

        if (this.size != this.baseSize) {
            value.putInt("size", this.size);
        }

        value.store("hidden_slots", ExtraCodecs.BIT_SET, this.hiddenSlots);
    }

    public void readData(ValueInput value, Consumer<ItemStack> dropped) {
        this.clearModifiers();

        value.listOrEmpty("persistent_modifiers", AttributeModifier.CODEC).forEach(this::addModifiers);
        value.listOrEmpty("cached_modifiers", AttributeModifier.CODEC).forEach(m -> {
            this.cachedModifiers.add(m);
            this.addModifierInternal(m);
        });
        this.update();

        ContainerSavingHelper.loadAllItems(value, this.stacks, dropped);
        ContainerSavingHelper.loadAllItems("cosmetic", value, this.cosmeticStacks, dropped);

        this.hiddenSlots = value.read("hidden_slots", ExtraCodecs.BIT_SET).orElse(new BitSet(this.size));
    }

    public void readDataV0(ValueInput value, Consumer<ItemStack> dropped) {
        this.clearModifiers();

        var metadata = value.child("Metadata");
        if (metadata.isPresent()) {
            value.listOrEmpty("PersistentModifiers", AttributeModifier.CODEC).forEach(this::addModifiers);
            value.listOrEmpty("CachedModifiers", AttributeModifier.CODEC).forEach(m -> {
                this.cachedModifiers.add(m);
                this.addModifierInternal(m);
            });
        }
        this.update();

        var items = value.read("Items", ItemStack.OPTIONAL_CODEC.listOf());
        if (items.isPresent()) {
            for (int i = 0; i < items.get().size(); i++) {
                if (i < this.stacks.size()) {
                    this.setItem(i, items.get().get(i));
                } else {
                    dropped.accept(items.get().get(i));
                }
            }
        }
    }

    public BitSet copyHiddenSlots() {
        var bitset = new BitSet(this.hiddenSlots.size());
        for (int i = 0; i < this.size; i++) {
            bitset.set(i, this.hiddenSlots.get(i));
        }
        return bitset;
    }

    public interface InventorySizeChangedCallback {
        void callSizeChanged(TrinketInventoryImpl inventory, int oldCount, int newCount);
    }
}