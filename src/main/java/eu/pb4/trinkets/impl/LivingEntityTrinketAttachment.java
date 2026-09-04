package eu.pb4.trinkets.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.impl.platform.CommonAbstraction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LivingEntityTrinketAttachment implements TrinketAttachment {
    private final Set<TrinketInventoryImpl> containerSizeChanged = new HashSet<>();
    public Map<String, TrinketInventoryImpl> inventory = new HashMap<>();
    public Map<String, SlotGroup> groups = new HashMap<>();
    public int size;
    public LivingEntity entity;
    @Deprecated
    private Map<String, Map<String, TrinketInventoryImpl>> legacyInventory = new HashMap<>();
    @Nullable
    private NonNullList<ItemStack> delayedDropped;

    public LivingEntityTrinketAttachment(LivingEntity entity) {
        this.entity = entity;
        this.rebuild();
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
    @Deprecated
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        //noinspection unchecked
        return (Map<String, Map<String, TrinketInventory>>) (Object) Collections.unmodifiableMap(legacyInventory);
    }

    public Map<String, TrinketInventory> getInventories() {
        //noinspection unchecked
        return Collections.unmodifiableMap(inventory);
    }

    @Override
    public @Nullable TrinketInventoryImpl getInventory(String slotId) {
        return this.inventory.get(slotId);
    }

    @Override
    public @Nullable TrinketSlotAccess getSlotAccess(String slotId, int slot) {
        var inv = getInventory(slotId);
        return inv != null ? inv.getSlotAccess(slot) : null;
    }

    public Map<String, Map<String, TrinketInventoryImpl>> getLegacyInventoryImpl() {
        return legacyInventory;
    }

    public void rebuild() {
        Map<String, SlotGroup> entitySlots = SlotGroup.getEntityGroups(this.entity);
        int count = 0;
        groups.clear();
        Map<TrinketSlotAccess, ItemStack> droppedItems = new HashMap<>();
        Map<String, Map<String, TrinketInventoryImpl>> legacyInventory = new HashMap<>();
        var inventory = new HashMap<String, TrinketInventoryImpl>();

        for (Map.Entry<String, SlotGroup> group : entitySlots.entrySet()) {
            String groupKey = group.getKey();
            SlotGroup groupValue = group.getValue();
            var oldGroup = this.legacyInventory.get(groupKey);
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
                                TrinketSlotAccess ref = oldInv.getSlotAccess(i);
                                if (ref == null) {
                                    continue;
                                }
                                ItemStack oldStack = stack;
                                if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory && !stackHistory.trinkets$getOldStack(ref).isEmpty()) {
                                    oldStack = stackHistory.trinkets$getOldStack(ref);
                                }
                                droppedItems.put(ref, oldStack);
                                if (this.entity instanceof Player player && !player.level().isClientSide()) {
                                    player.getInventory().placeItemBackInInventory(stack.copy());
                                } else if (this.entity.level() instanceof ServerLevel serverWorld) {
                                    this.entity.spawnAtLocation(serverWorld, stack);
                                }
                            }
                        }

                        if (inv.cosmeticItemsEnabled()) {
                            for (int i = 0; i < oldInv.cosmeticStacks.size(); i++) {
                                ItemStack stack = oldInv.cosmeticStacks.get(i).copy();
                                oldInv.cosmeticStacks.set(i, ItemStack.EMPTY);
                                if (i < inv.getContainerSize()) {
                                    inv.setCosmeticItem(i, stack);
                                } else {
                                    TrinketSlotAccess ref = oldInv.getOrCreateCosmeticSlotAccess(i);
                                    if (ref == null) {
                                        continue;
                                    }
                                    ItemStack oldStack = stack;
                                    if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory && !stackHistory.trinkets$getOldStack(ref).isEmpty()) {
                                        oldStack = stackHistory.trinkets$getOldStack(ref);
                                    }
                                    droppedItems.put(ref, oldStack);
                                    if (this.entity instanceof Player player && !player.level().isClientSide()) {
                                        player.getInventory().placeItemBackInInventory(stack.copy());
                                    } else if (this.entity.level() instanceof ServerLevel serverWorld) {
                                        this.entity.spawnAtLocation(serverWorld, stack);
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < oldInv.cosmeticStacks.size(); i++) {
                                var oldStack = oldInv.cosmeticStacks.get(i);
                                droppedItems.put(oldInv.getOrCreateCosmeticSlotAccess(i), oldStack);
                                if (this.entity instanceof Player player && !player.level().isClientSide()) {
                                    player.getInventory().placeItemBackInInventory(oldStack.copy());
                                } else if (this.entity.level() instanceof ServerLevel serverWorld) {
                                    this.entity.spawnAtLocation(serverWorld, oldStack);
                                }
                            }
                            oldInv.cosmeticStacks.clear();
                        }
                        oldInv.isValid = false;
                    }
                }

                legacyInventory.computeIfAbsent(group.getKey(), _ -> new HashMap<>()).put(slot.getKey(), inv);
                inventory.put(slot.getValue().getId(), inv);

                count += inv.getContainerSize();
            }
        }

        size = count;
        this.legacyInventory = legacyInventory;
        this.inventory = inventory;

        for (Map.Entry<TrinketSlotAccess, ItemStack> dropped : droppedItems.entrySet()) {
            try {
                TrinketUtilities.callTrinketEquipmentChange(dropped.getValue(), ItemStack.EMPTY, dropped.getKey(), entity);
                if (this.entity.level() instanceof ServerLevel) {
                    this.stopTrinketLocationBasedEffects(dropped.getValue(), dropped.getKey(), entity.getAttributes());
                }
                if (entity instanceof LivingEntityTrinketAttachment.StackHistory stackHistory) {
                    stackHistory.trinkets$resolveOldStack(dropped.getKey());
                }
                dropped.getKey().set(ItemStack.EMPTY);
            } catch (Exception e) {
                TrinketsMain.LOGGER.warn("Caught exception when dropping {} from removed slot {}.", dropped.getValue(), dropped.getKey().getSerializedName());
            }
        }
    }

    private void onSingleInventorySizeChanged(TrinketInventoryImpl inventory, int oldSize, int newSize) {
        this.containerSizeChanged.add(inventory);
        this.size += newSize - oldSize;
    }

    public void clearCachedModifiers() {
        for (var inv : this.inventory.values()) {
            inv.clearCachedModifiers();
        }
    }

    public Set<TrinketInventoryImpl> getContainerSizeChanged() {
        return this.containerSizeChanged;
    }

    public void addModifiers(String slotId, List<AttributeModifier> modifiers) {
        var inventory = this.getInventory(slotId);
        if (inventory != null) {
            modifiers.forEach(inventory::addSlotCountModifier);
        }
    }

    public void removeModifiers(String slotId, List<AttributeModifier> modifiers) {
        var inventory = this.getInventory(slotId);
        if (inventory != null) {
            modifiers.forEach(id -> inventory.removeSlotCountModifier(id.id()));
        }
    }

    public void clearModifiers() {
        for (var inv : this.inventory.values()) {
            inv.clearModifiers();
        }
    }

    public void stopTrinketLocationBasedEffects(final ItemStack oldStack, final TrinketSlotAccess inSlot, final AttributeMap attributes) {
        // MC-272769 Mitigation.
        Multimap<Holder<Attribute>, AttributeModifier> existsElsewhere = HashMultimap.create();

        if (inSlot == null) {
            return;
        }

        if (!oldStack.isEmpty()) {
            this.forEach(((slotReference, itemStack) -> {
                // We check type and index separately, as equals would depend on the inventory being the same as well.
                if (slotReference != null && !(slotReference.slotType().equals(inSlot.slotType()) && slotReference.index() == inSlot.index()) && !itemStack.isEmpty() && slotReference.canApplyEffects(itemStack)) {
                    TrinketUtilities.forEachModifier(entity, itemStack, slotReference, existsElsewhere::put);
                }
            }));
        }

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
        if (!inSlot.canApplyEffects(newStack)) {
            return;
        }
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
        NonNullList<ItemStack> dropped = NonNullList.create();

        var version = view.getIntOr("__version", 0);

        if (version == 0) {
            for (var groupKey : CommonAbstraction.INSTANCE.keys(view)) {
                var groupView = view.childOrEmpty(groupKey);
                for (var slotKey : CommonAbstraction.INSTANCE.keys(groupView)) {
                    var inv = this.inventory.get(groupKey + "/" + slotKey);
                    var value = groupView.childOrEmpty(slotKey);

                    if (inv != null) {
                        inv.readDataV0(value, dropped::add);
                    } else {
                        value.listOrEmpty("Items", ItemStack.OPTIONAL_CODEC).stream()
                                .filter(x -> !x.isEmpty()).forEach(dropped::add);
                    }
                }
            }
        } else if (version == 1) {
            var unhandled = new HashSet<>(this.inventory.keySet());

            for (var key : CommonAbstraction.INSTANCE.keys(view)) {
                if (key.equals("__version")) {
                    continue;
                }

                var inv = this.inventory.get(key);
                var value = view.childOrEmpty(key);

                if (inv != null) {
                    inv.readData(value, dropped::add);
                } else {
                    ContainerSavingHelper.loadAllItems(value, dropped::add);
                    ContainerSavingHelper.loadAllItems("cosmetic", value, dropped::add);
                }

                unhandled.remove(key);
            }

            for (var key : unhandled) {
                var inv = this.inventory.get(key);
                inv.clearContent();
                inv.clearModifiers();
            }
        }

        if (this.entity.level() instanceof ServerLevel serverWorld) {
            this.delayedDropped = dropped;
        }

        var slotMap = HashMultimap.<String, AttributeModifier>create();
        this.forEach((ref, stack) -> {
            if (!stack.isEmpty() && ref.canApplyEffects(stack)) {
                TrinketUtilities.forEachModifier(entity, stack, ref, (entityAttribute, value) -> {
                    if (entityAttribute.isBound() && entityAttribute.value() instanceof SlotAttributes.SlotModifyingAttribute slotEntityAttribute) {
                        slotMap.put(slotEntityAttribute.slot, value);
                    }
                });
            }
        });

        for (var inventory : this.inventory.values()) {
            var key = inventory.slotType().getId();
            Collection<AttributeModifier> modifiers = slotMap.get(key);
            for (AttributeModifier modifier : modifiers) {
                inventory.removeCachedModifier(modifier);
            }
            inventory.clearCachedModifiers();
        }
    }

    public void writeData(ValueOutput view) {
        view.putInt("__version", 1);
        for (var key : this.inventory.keySet()) {
            var inv = this.inventory.get(key);
            if (inv.skipSaving()) {
                continue;
            }

            var value = view.child(key);
            inv.writeData(value);
        }
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate, boolean requireActive) {
        for (var inv : this.inventory.values()) {
            if (requireActive && inv.slotType().isVanityOnly()) {
                continue;
            }

            for (int i = 0; i < inv.getContainerSize(); i++) {
                var access = inv.getOrCreateSlotAccess(i);
                if (predicate.test(access.get()) && (!requireActive || TrinketsApi.canApplyEffects(access.get(), access, this.entity))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<TrinketSlotAccess> equipped(Predicate<ItemStack> predicate, boolean requireActive) {
        List<TrinketSlotAccess> list = new ArrayList<>();
        forEach((slotReference, itemStack) -> {
            if (predicate.test(itemStack) && (!requireActive || TrinketsApi.canApplyEffects(itemStack, slotReference, this.entity))) {
                list.add(slotReference);
            }
        });
        return list;
    }

    @SuppressWarnings("removal")
    @Override
    @Deprecated(forRemoval = true)
    public List<Tuple<TrinketSlotAccess, ItemStack>> getEquipped(Predicate<ItemStack> predicate, boolean requireActive) {
        List<Tuple<TrinketSlotAccess, ItemStack>> list = new ArrayList<>();
        forEach((slotReference, itemStack) -> {
            if (predicate.test(itemStack) && (!requireActive || TrinketsApi.canApplyEffects(itemStack, slotReference, this.entity))) {
                list.add(new Tuple<>(slotReference, itemStack));
            }
        });
        return list;
    }

    @Override
    public Optional<TrinketSlotAccess> findFirst(Predicate<ItemStack> predicate, boolean requireActive) {
        for (var inv : this.inventory.values()) {
            if (requireActive && inv.slotType().isVanityOnly()) {
                continue;
            }

            for (int i = 0; i < inv.getContainerSize(); i++) {
                var access = inv.getOrCreateSlotAccess(i);
                if (predicate.test(access.get()) && (!requireActive || TrinketsApi.canApplyEffects(access.get(), access, this.entity))) {
                    return Optional.of(access);
                }
            }

            if (!requireActive && inv.hasCosmeticItems()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    var access = inv.getOrCreateCosmeticSlotAccess(i);
                    if (predicate.test(access.get())) {
                        return Optional.of(access);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void forEach(BiConsumer<TrinketSlotAccess, ItemStack> consumer) {
        for (var inv : this.inventory.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                consumer.accept(inv.getSlotAccess(i), inv.getItem(i));
            }

            if (inv.hasCosmeticItems()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    consumer.accept(inv.getCosmeticSlotAccess(i), inv.getCosmeticItem(i));
                }
            }
        }
    }

    @Override
    public void forEachWhileTrue(BiPredicate<TrinketSlotAccess, ItemStack> consumer) {
        for (var inv : this.inventory.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!consumer.test(inv.getSlotAccess(i), inv.getItem(i))) {
                    return;
                }
            }

            if (inv.hasCosmeticItems()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (!consumer.test(inv.getCosmeticSlotAccess(i), inv.getCosmeticItem(i))) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void forEach(Consumer<TrinketSlotAccess> consumer) {
        for (var inv : this.inventory.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                consumer.accept(inv.getSlotAccess(i));
            }

            if (inv.hasCosmeticItems()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    consumer.accept(inv.getCosmeticSlotAccess(i));
                }
            }
        }
    }

    @Override
    public void forEachWhileTrue(Predicate<TrinketSlotAccess> consumer) {
        for (var inv : this.inventory.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!consumer.test(inv.getSlotAccess(i))) {
                    return;
                }
            }

            if (inv.hasCosmeticItems()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (!consumer.test(inv.getCosmeticSlotAccess(i))) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void forEachVisible(BiConsumer<TrinketSlotAccess, ItemStack> consumer) {
        for (var inv : this.inventory.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.isVisible(i)) {
                    if (inv.hasCosmeticItems()) {
                        var deco = inv.getCosmeticItem(i);
                        if (!deco.isEmpty()) {
                            consumer.accept(inv.getCosmeticSlotAccess(i), deco);
                            continue;
                        }
                    }

                    consumer.accept(inv.getSlotAccess(i), inv.getItem(i));
                }
            }
        }
    }

    public void forEachDroppable(BiConsumer<TrinketSlotAccess, ItemStack> consumer) {
        this.forEachDroppable(consumer, this.entity instanceof ServerPlayer player && player.level().getGameRules().get(GameRules.KEEP_INVENTORY));
    }

    @Override
    public void forEachDroppable(BiConsumer<TrinketSlotAccess, ItemStack> consumer, boolean keepInventory) {
        for (var inv : this.inventory.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var access = inv.getSlotAccess(i);
                var stack = inv.getItem(i);
                if (TrinketsApi.getDropRule(stack, access, this.entity, keepInventory) == TrinketDropRule.DROP) {
                    consumer.accept(access, stack);
                }
            }

            if (inv.hasCosmeticItems()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    var access = inv.getCosmeticSlotAccess(i);
                    var stack = inv.getCosmeticItem(i);
                    if (TrinketsApi.getDropRule(stack, access, this.entity, keepInventory) == TrinketDropRule.DROP) {
                        consumer.accept(access, stack);
                    }
                }
            }
        }
    }

    @Override
    public boolean canApplyEffects(ItemStack itemStack, TrinketSlotAccess trinketSlotAccess) {
        return TrinketsApi.canApplyEffects(itemStack, trinketSlotAccess, this.entity);
    }

    public void tick() {
        if (this.delayedDropped != null) {
            for (var stack : this.delayedDropped) {
                if (this.entity instanceof Player player && !player.level().isClientSide()) {
                    player.getInventory().placeItemBackInInventory(stack);
                } else if (this.entity.level() instanceof ServerLevel serverWorld) {
                    this.entity.spawnAtLocation(serverWorld, stack);
                }
            }

            this.delayedDropped = null;
        }

        for (var inv : this.inventory.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var stack = inv.getItem(i);
                if (stack.isEmpty()) continue;

                TrinketCallback.getCallback(stack).tick(stack, inv.getSlotAccess(i), this.entity);
            }

        }
    }

    public void clearContents() {
        for (var x : this.inventory.values()) {
            x.clearContent();
        }
    }

    public interface Provider {
        LivingEntityTrinketAttachment trinkets$getAttachment();
    }

    public interface StackHistory {
        default ItemStack trinkets$getOldStack(TrinketSlotAccess ref) {
            return ItemStack.EMPTY;
        }

        default void trinkets$resolveOldStack(TrinketSlotAccess ref) {
        }
    }
}