package eu.pb4.trinkets.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import com.mojang.serialization.DynamicOps;
import eu.pb4.trinkets.impl.TrinketModifiers;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

public class LivingEntityTrinketComponent implements TrinketComponent, AutoSyncedComponent, RespawnableComponent {

	public Map<String, Map<String, TrinketInventory>> inventory = new HashMap<>();
	public Set<TrinketInventory> trackingUpdates = new HashSet<>();
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
		return inventory;
	}

	@Override
	public void update() {
		Map<String, SlotGroup> entitySlots = TrinketsApi.getEntitySlots(this.entity);
		int count = 0;
		groups.clear();
		Map<String, Map<String, TrinketInventory>> inventory = new HashMap<>();
		for (Map.Entry<String, SlotGroup> group : entitySlots.entrySet()) {
			String groupKey = group.getKey();
			SlotGroup groupValue = group.getValue();
			Map<String, TrinketInventory> oldGroup = this.inventory.get(groupKey);
			groups.put(groupKey, groupValue);
			for (Map.Entry<String, SlotType> slot : groupValue.getSlots().entrySet()) {
				TrinketInventory inv = new TrinketInventory(slot.getValue(), this, e -> this.trackingUpdates.add(e));
				if (oldGroup != null) {
					TrinketInventory oldInv = oldGroup.get(slot.getKey());
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

	@Override
	public void clearCachedModifiers() {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				slotType.getValue().clearCachedModifiers();
			}
		}
	}

	@Override
	public Set<TrinketInventory> getTrackingUpdates() {
		return this.trackingUpdates;
	}

	@Override
	public void addTemporaryModifiers(Multimap<String, AttributeModifier> modifiers) {
		for (Map.Entry<String, Collection<AttributeModifier>> entry : modifiers.asMap().entrySet()) {
			String[] keys = entry.getKey().split("/");
			String group = keys[0];
			String slot = keys[1];
			for (AttributeModifier modifier : entry.getValue()) {
				Map<String, TrinketInventory> groupInv = this.inventory.get(group);
				if (groupInv != null) {
					TrinketInventory inv = groupInv.get(slot);
					if (inv != null) {
						inv.addModifier(modifier);
					}
				}
			}
		}
	}

	@Override
	public void addPersistentModifiers(Multimap<String, AttributeModifier> modifiers) {
		for (Map.Entry<String, Collection<AttributeModifier>> entry : modifiers.asMap().entrySet()) {
			String[] keys = entry.getKey().split("/");
			String group = keys[0];
			String slot = keys[1];
			for (AttributeModifier modifier : entry.getValue()) {
				Map<String, TrinketInventory> groupInv = this.inventory.get(group);
				if (groupInv != null) {
					TrinketInventory inv = groupInv.get(slot);
					if (inv != null) {
						inv.addPersistentModifier(modifier);
					}
				}
			}
		}
	}

	@Override
	public void removeModifiers(Multimap<String, AttributeModifier> modifiers) {
		for (Map.Entry<String, Collection<AttributeModifier>> entry : modifiers.asMap().entrySet()) {
			String[] keys = entry.getKey().split("/");
			String group = keys[0];
			String slot = keys[1];
			for (AttributeModifier modifier : entry.getValue()) {
				Map<String, TrinketInventory> groupInv = this.inventory.get(group);
				if (groupInv != null) {
					TrinketInventory inv = groupInv.get(slot);
					if (inv != null) {
						inv.removeModifier(modifier.id());
					}
				}
			}
		}
	}

	@Override
	public Multimap<String, AttributeModifier> getModifiers() {
		Multimap<String, AttributeModifier> result = HashMultimap.create();
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				result.putAll(group.getKey() + "/" + slotType.getKey(), slotType.getValue().getModifiers().values());
			}
		}

		return result;
	}

	@Override
	public void clearModifiers() {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				slotType.getValue().clearModifiers();
			}
		}
	}

	@Override
	public void readData(ValueInput view) {
		Optional<TrinketSaveData> optional = view.read(TrinketSaveData.MAP_CODEC);
		NonNullList<ItemStack> dropped = NonNullList.create();
		if (optional.isPresent()) {
			TrinketSaveData data = optional.orElseThrow();
			for (String groupKey : data.data().keySet()) {
				Map<String, TrinketSaveData.InventoryData> groupTag = data.data().get(groupKey);
				if (groupTag != null) {
					Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);
					if (groupSlots != null) {
						for (String slotKey : groupTag.keySet()) {
							TrinketSaveData.InventoryData slotTag = groupTag.get(slotKey);
							TrinketInventory inv = groupSlots.get(slotKey);

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
		for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotEntry : groupEntry.getValue().entrySet()) {
				String group = groupEntry.getKey();
				String slot = slotEntry.getKey();
				String key = group + "/" + slot;
				Collection<AttributeModifier> modifiers = slotMap.get(key);
				TrinketInventory inventory = slotEntry.getValue();
				for (AttributeModifier modifier : modifiers) {
					inventory.removeCachedModifier(modifier);
				}
				inventory.clearCachedModifiers();
			}
		}
	}

	@Override
	public void applySyncPacket(RegistryFriendlyByteBuf buf) {
		CompoundTag tag = buf.readNbt();

		if (tag != null) {
			DynamicOps<Tag> ops = buf.registryAccess().createSerializationContext(NbtOps.INSTANCE);
			for (String groupKey : tag.keySet()) {
				CompoundTag groupTag = tag.getCompoundOrEmpty(groupKey);
				if (groupTag != null) {
					Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);
					if (groupSlots != null) {
						for (String slotKey : groupTag.keySet()) {
							CompoundTag slotTag = groupTag.getCompoundOrEmpty(slotKey);
							ListTag list = slotTag.getListOrEmpty("Items");
							TrinketInventory inv = groupSlots.get(slotKey);

							if (inv != null) {
								inv.applySyncMetadata(slotTag.read("Metadata", TrinketSaveData.Metadata.CODEC, ops).orElse(TrinketSaveData.Metadata.EMPTY));
							}

							for (int i = 0; i < list.size(); i++) {
								CompoundTag c = list.getCompoundOrEmpty(i);
								ItemStack stack = ItemStack.OPTIONAL_CODEC.decode(ops, c).result().map(com.mojang.datafixers.util.Pair::getFirst).orElse(ItemStack.EMPTY);
								if (inv != null && i < inv.getContainerSize()) {
									inv.setItem(i, stack);
								}
							}
						}
					}
				}
			}

			if (this.entity instanceof Player player) {
				((TrinketPlayerScreenHandler) player.inventoryMenu).trinkets$updateTrinketSlots(false);
			}
		}
	}

	@Override
	public void writeData(ValueOutput view) {
		TrinketSaveData data = new TrinketSaveData(new HashMap<>());
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			Map<String, TrinketSaveData.InventoryData> groupTag = new HashMap<>();
			for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
				TrinketInventory inv = slot.getValue();

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
	public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
		this.syncing = true;
		TagValueOutput tag = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
		this.writeData(tag);
		this.syncing = false;
		buf.writeNbt(tag.buildResult());
	}

	@Override
	public boolean shouldCopyForRespawn(boolean lossless, boolean keepInventory, boolean sameCharacter) {
		return lossless || keepInventory;
	}

	@Override
	public boolean isEquipped(Predicate<ItemStack> predicate) {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				TrinketInventory inv = slotType.getValue();
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
	public List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
		List<Tuple<SlotReference, ItemStack>> list = new ArrayList<>();
		forEach((slotReference, itemStack) -> {
			if (predicate.test(itemStack)) {
				list.add(new Tuple<>(slotReference, itemStack));
			}
		});
		return list;
	}

	@Override
	public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				TrinketInventory inv = slotType.getValue();
				for (int i = 0; i < inv.getContainerSize(); i++) {
					consumer.accept(new SlotReference(inv, i), inv.getItem(i));
				}
			}
		}
	}
}