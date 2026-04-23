package eu.pb4.trinkets.impl.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import eu.pb4.trinkets.impl.SlotTypeImpl;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.impl.data.SlotLoader.GroupData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

public class SlotLoader extends SimplePreparableReloadListener<Map<String, GroupData>> implements PreparableReloadListener {

	public static final SlotLoader INSTANCE = new SlotLoader();

	public static final Identifier ID = Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "slots");

	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	private static final int FILE_SUFFIX_LENGTH = ".json".length();

	private Map<String, GroupData> slots = new HashMap<>();

	@Override
	protected Map<String, GroupData> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Map<String, GroupData> map = new HashMap<>();
		String dataType = "slots";
		for (Map.Entry<Identifier, List<Resource>> entry : resourceManager.listResourceStacks(dataType, id -> id.getPath().endsWith(".json")).entrySet()) {
			Identifier identifier = entry.getKey();

			if (identifier.getNamespace().equals(TrinketsMain.NAMESPACE)) {

				try {
					for (Resource resource : entry.getValue()) {
						InputStreamReader reader = new InputStreamReader(resource.open());
						JsonObject jsonObject = GsonHelper.fromJson(GSON, reader, JsonObject.class);

						if (jsonObject != null) {
							String path = identifier.getPath();
							String[] parsed = path.substring(dataType.length() + 1, path.length() - FILE_SUFFIX_LENGTH).split("/", 2);
							String groupName = parsed[0];
							String fileName = parsed[parsed.length - 1];
							GroupData group = map.computeIfAbsent(groupName, (k) -> new GroupData());

							try {
								if (fileName.equals("group")) {
									group.read(jsonObject);
								} else {
									SlotData slot = group.slots.computeIfAbsent(fileName, (k) -> new SlotData());
									slot.read(jsonObject);
								}
							} catch (JsonSyntaxException e) {
								TrinketsMain.LOGGER.error("[trinkets] Syntax error while reading data for " + path);
								e.printStackTrace();
							}
						}
					}
				} catch (IOException e) {
					TrinketsMain.LOGGER.error("[trinkets] Unknown IO error while reading slot data!");
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	@Override
	protected void apply(Map<String, GroupData> loader, ResourceManager manager, ProfilerFiller profiler) {
		this.slots = loader;
	}

	Map<String, GroupData> getSlots() {
		return ImmutableMap.copyOf(this.slots);
	}

	public static class GroupData {

		private int slotId = -1;
		private int order = 0;
		private final Map<String, SlotData> slots = new HashMap<>();

		void read(JsonObject jsonObject) {
			slotId = GsonHelper.getAsInt(jsonObject, "slot_id", slotId);
			order = GsonHelper.getAsInt(jsonObject, "order", order);
		}

		int getSlotId() {
			return slotId;
		}

		int getOrder() {
			return order;
		}

		SlotData getSlot(String name) {
			return slots.get(name);
		}
	}

	static class SlotData {
		private static final Identifier DEFAULT_VALIDATOR_PREDICATES = Identifier.fromNamespaceAndPath("trinkets", "default");

		private int order = 0;
		private int amount = -1;
		private String icon = "";
		private SlotTypeImpl.Condition quickMovePredicates = null;
		private SlotTypeImpl.Condition validatorPredicates = null;
		private SlotTypeImpl.Condition tooltipPredicates = null;
		private String dropRule = TrinketDropRule.DEFAULT.toString();

		SlotType create(String group, String name) {
			Identifier finalIcon = icon == null || icon.isEmpty() ? null : Identifier.parse(icon);
			SlotTypeImpl.Condition finalValidatorPredicates = validatorPredicates;
			SlotTypeImpl.Condition finalQuickMovePredicates = quickMovePredicates;
			SlotTypeImpl.Condition finalTooltipPredicates = tooltipPredicates;

			if (finalValidatorPredicates == null) {
				finalValidatorPredicates = new SlotTypeImpl.DirectCondition(DEFAULT_VALIDATOR_PREDICATES);
			}
			if (finalQuickMovePredicates  == null) {
				finalQuickMovePredicates = new SlotTypeImpl.ConstantCondition(true);
			}
			if (finalTooltipPredicates  == null) {
				finalTooltipPredicates = new SlotTypeImpl.ConstantCondition(true);
			}

			if (amount == -1) {
				amount = 1;
			}
			return new SlotTypeImpl(group, name, order, amount, Optional.ofNullable(finalIcon), finalQuickMovePredicates, finalValidatorPredicates,
				finalTooltipPredicates, TrinketDropRule.valueOf(dropRule));
		}

		void read(JsonObject jsonObject) {
			boolean replace = GsonHelper.getAsBoolean(jsonObject, "replace", false);

			order = GsonHelper.getAsInt(jsonObject, "order", order);

			int jsonAmount = GsonHelper.getAsInt(jsonObject, "amount", amount);
			amount = replace ? jsonAmount : Math.max(jsonAmount, amount);

			icon = GsonHelper.getAsString(jsonObject, "icon", icon);

			quickMovePredicates = readPredicate(jsonObject, replace, "quick_move_predicates", quickMovePredicates);
			validatorPredicates = readPredicate(jsonObject, replace, "validator_predicates", validatorPredicates);
			tooltipPredicates = readPredicate(jsonObject, replace, "tooltip_predicates", tooltipPredicates);

			String jsonDropRule = GsonHelper.getAsString(jsonObject, "drop_rule", dropRule).toUpperCase();

			if (TrinketDropRule.has(jsonDropRule)) {
				dropRule = jsonDropRule;
			}
		}

		private SlotTypeImpl.@Nullable Condition readPredicate(JsonObject jsonObject, boolean replace, String type, SlotTypeImpl.Condition currentPredicate) {
			var predicate = Optional.ofNullable(jsonObject.get(type)).flatMap(x -> SlotTypeImpl.Condition.CODEC.decode(JsonOps.INSTANCE, x).result()).map(Pair::getFirst).orElse(null);
			var rule = Optional.ofNullable(jsonObject.getAsJsonPrimitive(type + ":merge_type")).map(JsonPrimitive::getAsString).orElse("or").toLowerCase(Locale.ROOT);
			replace |= rule.equals("replace") || rule.endsWith("override");

			if (replace || currentPredicate == null) {
				return predicate;
			}

			if (predicate == null) {
				return currentPredicate;
			}

			if (rule.equals("and")) {
				return new SlotTypeImpl.AndCondition(List.of(currentPredicate, predicate));
			}

			return new SlotTypeImpl.OrCondition(List.of(currentPredicate, predicate));
		}
	}
}
