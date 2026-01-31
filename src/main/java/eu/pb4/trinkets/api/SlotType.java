package eu.pb4.trinkets.api;

import eu.pb4.trinkets.api.TrinketEnums.DropRule;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class SlotType {

	private final String group;
	private final String name;
	private final int order;
	private final int amount;
	@Nullable
	private final Identifier icon;
	private final Set<Identifier> quickMovePredicates;
	private final Set<Identifier> validatorPredicates;
	private final Set<Identifier> tooltipPredicates;
	private final DropRule dropRule;

	public SlotType(String group, String name, int order, int amount, @Nullable Identifier icon, Set<Identifier> quickMovePredicates,
					Set<Identifier> validatorPredicates, Set<Identifier> tooltipPredicates, DropRule dropRule) {
		this.group = group;
		this.name = name;
		this.order = order;
		this.amount = amount;
		this.icon = icon;
		this.quickMovePredicates = quickMovePredicates;
		this.validatorPredicates = validatorPredicates;
		this.tooltipPredicates = tooltipPredicates;
		this.dropRule = dropRule;
	}

	public String getGroup() {
		return group;
	}

	public String getName() {
		return name;
	}

	public int getOrder() {
		return order;
	}

	public int getAmount() {
		return amount;
	}

	@Nullable
	public Identifier getIcon() {
		return icon;
	}

	public Set<Identifier> getQuickMovePredicates() {
		return quickMovePredicates;
	}

	public Set<Identifier> getValidatorPredicates() {
		return validatorPredicates;
	}

	public Set<Identifier> getTooltipPredicates() {
		return tooltipPredicates;
	}

	public DropRule getDropRule() {
		return dropRule;
	}

	public MutableComponent getTranslation() {
		return Component.translatable("trinkets.slot." + this.group + "." + this.name);
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
		DropRule dropRule = DropRule.DEFAULT;

		if (TrinketEnums.DropRule.has(dropRuleName)) {
			dropRule = TrinketEnums.DropRule.valueOf(dropRuleName);
		}
		return new SlotType(group, name, order, amount, icon, quickMovePredicates, validatorPredicates, tooltipPredicates, dropRule);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SlotType slotType = (SlotType) o;
		return group.equals(slotType.group) && name.equals(slotType.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(group, name);
	}

	public String getId() {
		return this.group + "/" + this.name;
	}
}
