package eu.pb4.trinkets.impl.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.impl.data.SlotLoader.GroupData;
import eu.pb4.trinkets.impl.data.SlotLoader.SlotData;
import eu.pb4.trinkets.impl.payload.SyncSlotsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;

public class EntitySlotLoader extends SimplePreparableReloadListener<Map<String, Map<String, Set<String>>>> implements IdentifiableResourceReloadListener {

	public static final EntitySlotLoader CLIENT = new EntitySlotLoader();
	public static final EntitySlotLoader SERVER = new EntitySlotLoader();

	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	private static final Identifier ID = Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "entities");

	private final Map<EntityType<?>, Map<String, SlotGroup>> slots = new HashMap<>();

	@Override
	protected Map<String, Map<String, Set<String>>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Map<String, Map<String, Set<String>>> map = new HashMap<>();
		String dataType = "entities";

		for (Map.Entry<Identifier, List<Resource>> entry : resourceManager.listResourceStacks(dataType, id -> id.getPath().endsWith(".json")).entrySet()) {
			Identifier identifier = entry.getKey();

			if (identifier.getNamespace().equals(TrinketsMain.MOD_ID)) {

				try {
					for (Resource resource : entry.getValue()) {
						InputStreamReader reader = new InputStreamReader(resource.open());
						JsonObject jsonObject = GsonHelper.fromJson(GSON, reader, JsonObject.class);

						if (jsonObject != null) {

							try {
								boolean replace = GsonHelper.getAsBoolean(jsonObject, "replace", false);
								JsonArray assignedSlots = GsonHelper.getAsJsonArray(jsonObject, "slots", new JsonArray());
								Map<String, Set<String>> groups = new HashMap<>();

								if (assignedSlots != null) {

									for (JsonElement assignedSlot : assignedSlots) {
										String slot = assignedSlot.getAsString();
										String[] parsedSlot = slot.split("/");

										if (parsedSlot.length != 2) {
											TrinketsMain.LOGGER.error("Detected malformed slot assignment " + slot
													+ "! Slots should be in the format 'group/slot'.");
											continue;
										}
										String group = parsedSlot[0];
										String name = parsedSlot[1];
										groups.computeIfAbsent(group, (k) -> new HashSet<>()).add(name);
									}
								}
								JsonArray entities = GsonHelper.getAsJsonArray(jsonObject, "entities", new JsonArray());

								if (!groups.isEmpty() && entities != null) {

									for (JsonElement entity : entities) {
										String name = entity.getAsString();
										String id;

										if (name.startsWith("#")) {
											id = "#" + Identifier.parse(name.substring(1));
										} else {
											id = Identifier.parse(name).toString();
										}
										Map<String, Set<String>> slots = map.computeIfAbsent(id, (k) -> new HashMap<>());

										if (replace) {
											slots.clear();
										}
										groups.forEach((groupName, slotNames) -> slots.computeIfAbsent(groupName, (k) -> new HashSet<>())
												.addAll(slotNames));
									}
								}
							} catch (JsonSyntaxException e) {
								TrinketsMain.LOGGER.error("[trinkets] Syntax error while reading data for " + identifier.getPath());
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
	protected void apply(Map<String, Map<String, Set<String>>> loader, ResourceManager manager, ProfilerFiller profiler) {
		Map<String, GroupData> slots = SlotLoader.INSTANCE.getSlots();
		Map<EntityType<?>, Map<String, SlotGroup.Builder>> groupBuilders = new HashMap<>();

		loader.forEach((entityName, groups) -> {
			Set<EntityType<?>> types = new HashSet<>();

			try {
				if (entityName.startsWith("#")) {
					// TODO rewrite this to work with the new tag system
					TrinketsMain.LOGGER.error("[trinkets] Attempted to assign entity entry to tag");
					/*
					TagKey<EntityType<?>> tag = TagKey.of(Registry.ENTITY_TYPE_KEY, Identifier.of(entityName.substring(1)));
					List<? extends EntityType<?>> entityTypes = Registry.ENTITY_TYPE.getEntryList(tag)
							.orElseThrow(() -> new IllegalArgumentException("Unknown entity tag '" + entityName + "'"))
							.stream()
							.map(RegistryEntry::value)
							.toList();

					types.addAll(entityTypes);*/
				} else {
					types.add(BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entityName))
							.orElseThrow(() -> new IllegalArgumentException("Unknown entity '" + entityName + "'")));
				}
			} catch (IllegalArgumentException e) {
				TrinketsMain.LOGGER.error("[trinkets] Attempted to assign unknown entity entry " + entityName);
			}

			for (EntityType<?> type : types) {
				Map<String, SlotGroup.Builder> builders = groupBuilders.computeIfAbsent(type, (k) -> new HashMap<>());
				groups.forEach((groupName, slotNames) -> {
					GroupData group = slots.get(groupName);

					if (group != null) {
						SlotGroup.Builder builder = builders.computeIfAbsent(groupName,
								(k) -> new SlotGroup.Builder(groupName, group.getSlotId(), group.getOrder()));
						slotNames.forEach(slotName -> {
							SlotData slotData = group.getSlot(slotName);

							if (slotData != null) {
								builder.addSlot(slotName, slotData.create(groupName, slotName));
							} else {
								TrinketsMain.LOGGER.error("[trinkets] Attempted to assign unknown slot " + slotName);
							}
						});
					} else {
						TrinketsMain.LOGGER.error("[trinkets] Attempted to assign slot from unknown group " + groupName);
					}
				});
			}
		});
		this.slots.clear();

		groupBuilders.forEach((entity, groups) -> {
			Map<String, SlotGroup> entitySlots = this.slots.computeIfAbsent(entity, (k) -> new HashMap<>());
			groups.forEach((groupName, groupBuilder) -> entitySlots.putIfAbsent(groupName, groupBuilder.build()));
		});
	}

	public Map<String, SlotGroup> getEntitySlots(EntityType<?> entityType) {
		if (this.slots.containsKey(entityType)) {
			return ImmutableMap.copyOf(this.slots.get(entityType));
		}
		return ImmutableMap.of();
	}

	public void setSlots(Map<EntityType<?>, Map<String, SlotGroup>> slots) {
		this.slots.clear();
		this.slots.putAll(slots);
	}

	public void sync(ServerPlayer playerEntity) {
		ServerPlayNetworking.send(playerEntity, new SyncSlotsPayload(Map.copyOf(this.slots)));
	}

	public void sync(List<? extends ServerPlayer> players) {
		SyncSlotsPayload packet = new SyncSlotsPayload(Map.copyOf(this.slots));
		players.forEach(player -> ServerPlayNetworking.send(player, packet));
		players.forEach(player -> ((TrinketPlayerScreenHandler) player.inventoryMenu).trinkets$updateTrinketSlots(true));
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return Lists.newArrayList(SlotLoader.ID);
	}
}