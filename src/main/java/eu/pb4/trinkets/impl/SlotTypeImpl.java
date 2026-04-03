package eu.pb4.trinkets.impl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record SlotTypeImpl(String group, String name, int order, int amount, @Nullable Identifier icon,
					   Set<Identifier> quickMovePredicates, Set<Identifier> validatorPredicates,
					   Set<Identifier> tooltipPredicates, TrinketDropRule dropRule)implements SlotType {

	@Override public MutableComponent getTranslation() {
		return Component.translatable("trinkets.slot." + this.group + "." + this.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SlotType slotType = (SlotType) o;
		return group.equals(slotType.group()) && name.equals(slotType.name());
	}

	public void write(CompoundTag data) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Group", group);
		tag.putString("Name", name);
		tag.putInt("Order", order);
		tag.putInt("Amount", amount);
		if (icon != null) {
			tag.putString("Icon", icon.toString());
		}
		ListTag quickMovePredicateList = new ListTag();

		for (Identifier id : quickMovePredicates) {
			quickMovePredicateList.add(StringTag.valueOf(id.toString()));
		}
		tag.put("QuickMovePredicates", quickMovePredicateList);

		ListTag validatorPredicateList = new ListTag();

		for (Identifier id : validatorPredicates) {
			validatorPredicateList.add(StringTag.valueOf(id.toString()));
		}
		tag.put("ValidatorPredicates", validatorPredicateList);

		ListTag tooltipPredicateList = new ListTag();

		for (Identifier id : tooltipPredicates) {
			tooltipPredicateList.add(StringTag.valueOf(id.toString()));
		}
		tag.put("TooltipPredicates", tooltipPredicateList);
		tag.putString("DropRule", dropRule.toString());
		data.put("SlotData", tag);
	}

	public static SlotType read(CompoundTag data) {
		CompoundTag slotData = data.getCompoundOrEmpty("SlotData");
		String group = slotData.getStringOr("Group", "");
		String name = slotData.getStringOr("Name", "");
		int order = slotData.getIntOr("Order", 0);
		int amount = slotData.getIntOr("Amount", 0);
		Identifier icon = slotData.read("Icon", Identifier.CODEC).orElse(null);
		ListTag quickMoveList = slotData.getListOrEmpty("QuickMovePredicates");
		Set<Identifier> quickMovePredicates = new HashSet<>();

		for (Tag tag : quickMoveList) {
			if (tag instanceof StringTag string) {
				quickMovePredicates.add(Identifier.parse(string.value()));
			}
		}
		ListTag validatorList = slotData.getListOrEmpty("ValidatorPredicates");
		Set<Identifier> validatorPredicates = new HashSet<>();

		for (Tag tag : validatorList) {
			if (tag instanceof StringTag string) {
				validatorPredicates.add(Identifier.parse(string.value()));
			}
		}
		ListTag tooltipList = slotData.getListOrEmpty("TooltipPredicates");
		Set<Identifier> tooltipPredicates = new HashSet<>();

		for (Tag tag : tooltipList) {
			if (tag instanceof StringTag string) {
				tooltipPredicates.add(Identifier.parse(string.value()));
			}
		}
		String dropRuleName = slotData.getStringOr("DropRule", "");
		TrinketDropRule dropRule = TrinketDropRule.DEFAULT;

		if (TrinketDropRule.has(dropRuleName)) {
			dropRule = TrinketDropRule.valueOf(dropRuleName);
		}
		return new SlotTypeImpl(group, name, order, amount, icon, quickMovePredicates, validatorPredicates, tooltipPredicates, dropRule);
	}

	@Override
	public int hashCode() {
		return Objects.hash(group, name);
	}

	@Override public String getId() {
		return this.group + "/" + this.name;
	}

	@Override
	public boolean quickMoveCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
		return TrinketUtilities.evaluatePredicateSet(this.quickMovePredicates, stack, slotRef, entity);
	}

	@Override
	public boolean validatorCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
		return TrinketUtilities.evaluatePredicateSet(this.validatorPredicates, stack, slotRef, entity);
	}

	@Override
	public boolean tooltipCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity) {
		return TrinketUtilities.evaluatePredicateSet(this.tooltipPredicates, stack, slotRef, entity);
	}
}
