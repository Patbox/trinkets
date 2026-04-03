package eu.pb4.trinkets.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.DynamicOps;
import eu.pb4.trinkets.api.*;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class LivingEntityTrinketComponent implements TrinketAttachment, RespawnableComponent, ComponentV3 {
    public static final ComponentKey<LivingEntityTrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
            .getOrCreate(Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "trinkets"), LivingEntityTrinketComponent.class);

    public Map<String, Map<String, TrinketInventoryImpl>> inventory = new HashMap<>();
    private final Set<TrinketInventoryImpl> containerSizeChanged = new HashSet<>();
    public Map<String, SlotGroup> groups = new HashMap<>();
    public int size;
    public LivingEntity entity;
    private boolean syncing;

    public LivingEntityTrinketComponent(LivingEntity entity) {
        this.entity = entity;
        this.update();
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

    @SuppressWarnings("removal")
    @Override
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
                                inv.fromMetadata(slotTag.metadata());
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
                Multimap<Holder<Attribute>, AttributeModifier> map = TrinketModifiers.get(stack, ref, entity);
                for (Holder<Attribute> entityAttribute : map.keySet()) {
                    if (entityAttribute.isBound() && entityAttribute.value() instanceof SlotAttributes.SlotEntityAttribute slotEntityAttribute) {
                        slotMap.putAll(slotEntityAttribute.slot, map.get(entityAttribute));
                    }
                }
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

    @Override
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
                TrinketSaveData.Metadata metadata = this.syncing ? inv.getSyncMetadata() : inv.toMetadata();
                groupTag.put(slot.getKey(), new TrinketSaveData.InventoryData(metadata, items));
            }
            data.data().put(group.getKey(), groupTag);
        }
        view.store(TrinketSaveData.MAP_CODEC, data);
    }

    @Override
    public boolean shouldCopyForRespawn(boolean lossless, boolean keepInventory, boolean sameCharacter) {
        return lossless || keepInventory;
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        for (Map.Entry<String, Map<String, TrinketInventoryImpl>> group : this.getInventoryImpl().entrySet()) {
            for (Map.Entry<String, TrinketInventoryImpl> slotType : group.getValue().entrySet()) {
                TrinketInventoryImpl inv = slotType.getValue();
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
        for (Map.Entry<String, Map<String, TrinketInventoryImpl>> group : this.getInventoryImpl().entrySet()) {
            for (Map.Entry<String, TrinketInventoryImpl> slotType : group.getValue().entrySet()) {
                TrinketInventoryImpl inv = slotType.getValue();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    consumer.accept(new TrinketSlotAccess(inv, i), inv.getItem(i));
                }
            }
        }
    }
}