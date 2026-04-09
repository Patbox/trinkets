package eu.pb4.trinkets.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.pb4.trinkets.api.*;
import net.minecraft.core.Holder;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class LivingEntityTrinketAttachment implements TrinketAttachment {
    private final Set<TrinketInventoryImpl> containerSizeChanged = new HashSet<>();
    public Map<String, Map<String, TrinketInventoryImpl>> inventory = new HashMap<>();
    public Map<String, SlotGroup> groups = new HashMap<>();
    public int size;
    public LivingEntity entity;

    public LivingEntityTrinketAttachment(LivingEntity entity) {
        this.entity = entity;
        this.update();
    }

    public static LivingEntityTrinketAttachment get(LivingEntity livingEntity) {
        return ((LivingEntityTrinketAttachment.Provider) livingEntity).trinkets$getAttachment();
    }

    public static void copyData(LivingEntity from, LivingEntity to, ConversionParams conversionParams) {
        if (!conversionParams.keepEquipment()) {
            return;
        }

        copyData(from, to);
    }

    public static void copyData(LivingEntity from, LivingEntity to, boolean restoreAll) {
        copyData(from, to);
    }

    public static void copyData(LivingEntity from, LivingEntity to) {
        try (var errorReporter = new ProblemReporter.ScopedCollector(TrinketsMain.LOGGER)) {
            TagValueOutput writeView = TagValueOutput.createWithContext(errorReporter, from.registryAccess());
            get(from).writeData(writeView);
            get(to).readData(TagValueInput.create(errorReporter, to.registryAccess(), writeView.buildResult()));
        }
    }

    @Override
    public LivingEntity getEntity() {
        return this.entity;
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return this.groups;
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        //noinspection unchecked
        return (Map<String, Map<String, TrinketInventory>>) (Object) Collections.unmodifiableMap(inventory);
    }

    public Map<String, Map<String, TrinketInventoryImpl>> getInventoryImpl() {
        return inventory;
    }

    public void update() {
        Map<String, SlotGroup> entitySlots = TrinketsApi.getEntitySlots(this.entity);
        int count = 0;
        groups.clear();
        Map<String, Map<String, TrinketInventoryImpl>> inventory = new HashMap<>();
        for (Map.Entry<String, SlotGroup> group : entitySlots.entrySet()) {
            String groupKey = group.getKey();
            SlotGroup groupValue = group.getValue();
            Map<String, TrinketInventoryImpl> oldGroup = this.inventory.get(groupKey);
            groups.put(groupKey, groupValue);
            for (Map.Entry<String, SlotType> slot : groupValue.slots().entrySet()) {
                TrinketInventoryImpl inv = new TrinketInventoryImpl(slot.getValue(), this, _ -> {
                }, this::onSingleInventorySizeChanged, entity.level().isClientSide());
                if (oldGroup != null) {
                    TrinketInventoryImpl oldInv = oldGroup.get(slot.getKey());
                    if (oldInv != null) {
                        inv.copyFrom(oldInv);
                        for (int i = 0; i < oldInv.getContainerSize(); i++) {
                            ItemStack stack = oldInv.getItem(i).copy();
                            if (i < inv.getContainerSize()) {
                                inv.setItem(i, stack);
                            } else {
                                TrinketSlotAccess ref = new TrinketSlotAccess(oldInv, i);
                                ItemStack oldStack = stack;
                                if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory && !stackHistory.trinkets$getOldStack(ref).isEmpty()) {
                                    oldStack = stackHistory.trinkets$getOldStack(ref);
                                }
                                if (this.entity.level() instanceof ServerLevel serverWorld) {
                                    this.stopTrinketLocationBasedEffects(oldStack, ref, entity.getAttributes());
                                }
                                if (this.entity instanceof Player player) {
                                    player.getInventory().placeItemBackInInventory(stack);
                                } else if (this.entity.level() instanceof ServerLevel serverWorld) {
                                    this.entity.spawnAtLocation(serverWorld, stack);
                                }
                            }
                        }
                    }
                }
                inventory.computeIfAbsent(group.getKey(), (k) -> new HashMap<>()).put(slot.getKey(), inv);
                count += inv.getContainerSize();
            }
        }
        size = count;
        this.inventory = inventory;
    }

    private void onSingleInventorySizeChanged(TrinketInventoryImpl inventory, int oldSize, int newSize) {
        this.containerSizeChanged.add(inventory);
        this.size += newSize - oldSize;
    }

    public void clearCachedModifiers() {
        for (Map.Entry<String, Map<String, TrinketInventoryImpl>> group : this.getInventoryImpl().entrySet()) {
            for (Map.Entry<String, TrinketInventoryImpl> slotType : group.getValue().entrySet()) {
                slotType.getValue().clearCachedModifiers();
            }
        }
    }

    public Set<TrinketInventoryImpl> getContainerSizeChanged() {
        return this.containerSizeChanged;
    }

    public void addModifiers(String slotId, List<AttributeModifier> modifiers) {
        String[] keys = slotId.split("/");
        String group = keys[0];
        String slot = keys[1];
        for (AttributeModifier modifier : modifiers) {
            Map<String, TrinketInventoryImpl> groupInv = this.inventory.get(group);
            if (groupInv != null) {
                TrinketInventoryImpl inv = groupInv.get(slot);
                if (inv != null) {
                    inv.addModifiers(modifier);
                }
            }
        }
    }

    public void removeModifiers(String slotId, List<AttributeModifier> modifiers) {
        String[] keys = slotId.split("/");
        String group = keys[0];
        String slot = keys[1];
        for (AttributeModifier modifier : modifiers) {
            Map<String, TrinketInventoryImpl> groupInv = this.inventory.get(group);
            if (groupInv != null) {
                TrinketInventoryImpl inv = groupInv.get(slot);
                if (inv != null) {
                    inv.removeModifier(modifier.id());
                }
            }
        }
    }

    public void clearModifiers() {
        for (Map.Entry<String, Map<String, TrinketInventoryImpl>> group : this.getInventoryImpl().entrySet()) {
            for (Map.Entry<String, TrinketInventoryImpl> slotType : group.getValue().entrySet()) {
                slotType.getValue().clearModifiers();
            }
        }
    }

    public void stopTrinketLocationBasedEffects(final ItemStack oldStack, final TrinketSlotAccess inSlot, final AttributeMap attributes) {
        // MC-272769 Mitigation.
        Multimap<Holder<Attribute>, AttributeModifier> existsElsewhere = HashMultimap.create();
        this.forEach(((slotReference, itemStack) -> {
            if (!slotReference.equals(inSlot) && !itemStack.isEmpty()) {
                TrinketUtilities.forEachModifier(entity, itemStack, slotReference, existsElsewhere::put);
            }
        }));

        TrinketUtilities.forEachModifier(entity, oldStack, inSlot, (attribute, modifier) -> {
            if (existsElsewhere.containsEntry(attribute, modifier)) {
                return;
            }

            if (attribute.value() instanceof SlotAttributes.SlotModifyingAttribute x) {
                this.removeModifiers(x.slot, List.of(modifier));
                return;
            }

            AttributeInstance instance = attributes.getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier);
            }
        });

        TrinketUtilities.runIterationOnItem(oldStack, inSlot, entity, (enchantment, level, item) -> enchantment.value().stopLocationBasedEffects(level, item, entity));
    }

    public void addSlotModifiers(final ItemStack newStack, final TrinketSlotAccess inSlot, final AttributeMap attributes) {
        TrinketUtilities.forEachModifier(entity, newStack, inSlot, (attribute, modifier) -> {
            if (attribute.value() instanceof SlotAttributes.SlotModifyingAttribute x) {
                this.addModifiers(x.slot, List.of(modifier));
                return;
            }

            AttributeInstance instance = attributes.getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
                instance.addTransientModifier(modifier);
            }

        });
        if (!newStack.isEmpty() && !newStack.isBroken()) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                TrinketUtilities.runIterationOnItem(newStack, inSlot, entity, (enchantment, level, item) -> enchantment.value().runLocationChangedEffects(serverLevel, level, item, entity));
            }
        }
    }

    @SuppressWarnings("removal")
    public void readData(ValueInput view) {
        Optional<TrinketSaveData> optional = view.read(TrinketSaveData.MAP_CODEC);
        NonNullList<ItemStack> dropped = NonNullList.create();
        if (optional.isPresent()) {
            TrinketSaveData data = optional.orElseThrow();
            for (String groupKey : data.data().keySet()) {
                Map<String, TrinketSaveData.InventoryData> groupTag = data.data().get(groupKey);
                if (groupTag != null) {
                    Map<String, TrinketInventoryImpl> groupSlots = this.inventory.get(groupKey);
                    if (groupSlots != null) {
                        for (String slotKey : groupTag.keySet()) {
                            TrinketSaveData.InventoryData slotTag = groupTag.get(slotKey);
                            TrinketInventoryImpl inv = groupSlots.get(slotKey);

                            if (inv != null) {
                                inv.fromMetadata(slotTag.metadata(), slotTag.inventorySize());
                            }

                            for (int i = 0; i < slotTag.items().size(); i++) {
                                ItemStack stack = slotTag.items().get(i);
                                if (inv != null && i < inv.getContainerSize()) {
                                    inv.setItem(i, stack);
                                } else {
                                    dropped.add(stack);
                                }
                            }
                        }
                    } else {
                        for (String slotKey : groupTag.keySet()) {
                            dropped.addAll(groupTag.get(slotKey).items());
                        }
                    }
                }
            }
        }
        if (this.entity.level() instanceof ServerLevel serverWorld) {
            for (ItemStack itemStack : dropped) {
                this.entity.spawnAtLocation(serverWorld, itemStack);
            }
        }
        Multimap<String, AttributeModifier> slotMap = HashMultimap.create();
        this.forEach((ref, stack) -> {
            if (!stack.isEmpty()) {
                TrinketUtilities.forEachModifier(entity, stack, ref, (entityAttribute, value) -> {
                    if (entityAttribute.isBound() && entityAttribute.value() instanceof SlotAttributes.SlotModifyingAttribute slotEntityAttribute) {
                        slotMap.put(slotEntityAttribute.slot, value);
                    }
                });
            }
        });
        for (Map.Entry<String, Map<String, TrinketInventoryImpl>> groupEntry : this.getInventoryImpl().entrySet()) {
            for (Map.Entry<String, TrinketInventoryImpl> slotEntry : groupEntry.getValue().entrySet()) {
                String group = groupEntry.getKey();
                String slot = slotEntry.getKey();
                String key = group + "/" + slot;
                Collection<AttributeModifier> modifiers = slotMap.get(key);
                TrinketInventoryImpl inventory = slotEntry.getValue();
                for (AttributeModifier modifier : modifiers) {
                    inventory.removeCachedModifier(modifier);
                }
                inventory.clearCachedModifiers();
            }
        }
    }

    public void writeData(ValueOutput view) {
        TrinketSaveData data = new TrinketSaveData(new HashMap<>());
        for (Map.Entry<String, Map<String, TrinketInventoryImpl>> group : this.getInventoryImpl().entrySet()) {
            Map<String, TrinketSaveData.InventoryData> groupTag = new HashMap<>();
            for (Map.Entry<String, TrinketInventoryImpl> slot : group.getValue().entrySet()) {
                TrinketInventoryImpl inv = slot.getValue();

                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    items.add(inv.getItem(i).copy());
                }
                var metadata = inv.toMetadata();
                groupTag.put(slot.getKey(), new TrinketSaveData.InventoryData(metadata, items, inv.getSize()));
            }
            data.data().put(group.getKey(), groupTag);
        }
        view.store(TrinketSaveData.MAP_CODEC, data);
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        for (var group : this.inventory.entrySet()) {
            for (var slotType : group.getValue().entrySet()) {
                var inv = slotType.getValue();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (predicate.test(inv.getItem(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<Tuple<TrinketSlotAccess, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        List<Tuple<TrinketSlotAccess, ItemStack>> list = new ArrayList<>();
        forEach((slotReference, itemStack) -> {
            if (predicate.test(itemStack)) {
                list.add(new Tuple<>(slotReference, itemStack));
            }
        });
        return list;
    }

    @Override
    public void forEach(BiConsumer<TrinketSlotAccess, ItemStack> consumer) {
        for (var group : this.getInventoryImpl().values()) {
            for (var inv : group.values()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    consumer.accept(new TrinketSlotAccess(inv, i), inv.getItem(i));
                }
            }
        }
    }

    @Override
    public void forEachWhileTrue(BiPredicate<TrinketSlotAccess, ItemStack> consumer) {
        for (var group : this.getInventoryImpl().values()) {
            for (var inv : group.values()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (!consumer.test(new TrinketSlotAccess(inv, i), inv.getItem(i))) {
                        return;
                    }
                }
            }
        }
    }

    public void tick() {
        for (var group : this.inventory.values()) {
            for (var inv : group.values()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    var stack = inv.getItem(i);
                    if (stack.isEmpty()) continue;

                    TrinketCallback.getCallback(stack).tick(stack, new TrinketSlotAccess(inv, i), this.entity);
                }
            }
        }
    }

    public interface Provider {
        LivingEntityTrinketAttachment trinkets$getAttachment();
    }

    public interface StackHistory {
        default ItemStack trinkets$getOldStack(TrinketSlotAccess ref) {
            return ItemStack.EMPTY;
        }
    }
}