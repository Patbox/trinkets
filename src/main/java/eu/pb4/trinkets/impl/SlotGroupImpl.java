package eu.pb4.trinkets.impl;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import net.minecraft.nbt.CompoundTag;

public record SlotGroupImpl(String name, int slotId, int order, Map<String, SlotTypeImpl> slotsImpl) implements SlotGroup {

	@Override
	public Map<String, SlotType> slots() {
		return ImmutableMap.copyOf(this.slotsImpl);
	}

	public void write(CompoundTag data) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Name", name);
		tag.putInt("SlotId", slotId);
		tag.putInt("Order", order);
		CompoundTag typesTag = new CompoundTag();

		slotsImpl.forEach((id, slot) -> {
			CompoundTag typeTag = new CompoundTag();
			slot.write(typeTag);
			typesTag.put(id, typeTag);
		});
		tag.put("SlotTypes", typesTag);
		data.put("GroupData", tag);
	}

	public static SlotGroup read(CompoundTag data) {
		CompoundTag groupData = data.getCompoundOrEmpty("GroupData");
		String name = groupData.getStringOr("Name", "");
		int slotId = groupData.getIntOr("SlotId", 0);
		int order = groupData.getIntOr("Order", 0);
		CompoundTag typesTag = groupData.getCompoundOrEmpty("SlotTypes");
		Builder builder = new Builder(name, slotId, order);

		for (String id : typesTag.keySet()) {
			CompoundTag tag = (CompoundTag) typesTag.get(id);

			if (tag != null) {
				builder.addSlot(id, SlotTypeImpl.read(tag));
			}
		}
		return builder.build();
	}

	}
