package eu.pb4.trinkets.api;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

public final class SlotGroup {

	private final String name;
	private final int slotId;
	private final int order;
	private final Map<String, SlotType> slots;

	private SlotGroup(Builder builder) {
		this.name = builder.name;
		this.slots = builder.slots;
		this.slotId = builder.slotId;
		this.order = builder.order;
	}

	public int getSlotId() {
		return slotId;
	}

	public int getOrder() {
		return order;
	}

	public String getName() {
		return name;
	}

	public Map<String, SlotType> getSlots() {
		return ImmutableMap.copyOf(slots);
	}

	public void write(CompoundTag data) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Name", name);
		tag.putInt("SlotId", slotId);
		tag.putInt("Order", order);
		CompoundTag typesTag = new CompoundTag();

		slots.forEach((id, slot) -> {
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
				builder.addSlot(id, SlotType.read(tag));
			}
		}
		return builder.build();
	}

	public static class Builder {

		private final String name;
		private final int slotId;
		private final int order;
		private final Map<String, SlotType> slots = new HashMap<>();

		public Builder(String name, int slotId, int order) {
			this.name = name;
			this.slotId = slotId;
			this.order = order;
		}

		public Builder addSlot(String name, SlotType slot) {
			this.slots.put(name, slot);
			return this;
		}

		public SlotGroup build() {
			return new SlotGroup(this);
		}
	}
}
