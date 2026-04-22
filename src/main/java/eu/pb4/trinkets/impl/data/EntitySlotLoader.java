package eu.pb4.trinkets.impl.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.SlotGroupImpl;
import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.impl.data.SlotLoader.GroupData;
import eu.pb4.trinkets.impl.data.SlotLoader.SlotData;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import eu.pb4.trinkets.impl.payload.SyncSlotsPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;

public class EntitySlotLoader extends SimplePreparableReloadListener<Map<String, Map<String, Set<String>>>> implements PreparableReloadListener {

	public static final EntitySlotLoader CLIENT = new EntitySlotLoader();
	public static final EntitySlotLoader SERVER = new EntitySlotLoader();

	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	public static final Identifier ID = Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "entities");

	private final Map<EntityType<?>, Map<String, SlotGroupImpl>> slots = new HashMap<>();

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
										String[] parsedSlot = slot.split("/", 2);

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
		Map<EntityType<?>, Map<String, SlotGroupImpl.Builder>> groupBuilders = new HashMap<>();

		loader.forEach((entityName, groups) -> {
			Set<EntityType<?>> types = new HashSet<>();

			try {
				if (entityName.startsWith("#")) {
					var tag = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(entityName.substring(1)));
					BuiltInRegistries.ENTITY_TYPE.get(tag).ifPresent(x -> x.forEach(y -> types.add(y.value())));
				} else {
					types.add(BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entityName))
							.orElseThrow(() -> new IllegalArgumentException("Unknown entity '" + entityName + "'")));
				}
			} catch (Throwable e) {
				TrinketsMain.LOGGER.error("[trinkets] Attempted to assign unknown entity entry " + entityName);
			}

			for (EntityType<?> type : types) {
				Map<String, SlotGroupImpl.Builder> builders = groupBuilders.computeIfAbsent(type, (k) -> new HashMap<>());
				groups.forEach((groupName, slotNames) -> {
					GroupData group = slots.get(groupName);

					if (group != null) {
						var builder = builders.computeIfAbsent(groupName,
								(k) -> new SlotGroupImpl.Builder(groupName, group.getSlotId(), group.getOrder()));
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
			var entitySlots = this.slots.computeIfAbsent(entity, (k) -> new HashMap<>());
			groups.forEach((groupName, groupBuilder) -> entitySlots.putIfAbsent(groupName, groupBuilder.build()));
		});
	}

	public Map<String, SlotGroup> getEntitySlots(EntityType<?> entityType) {
		if (this.slots.containsKey(entityType)) {
			return ImmutableMap.copyOf(this.slots.get(entityType));
		}
		return ImmutableMap.of();
	}

	public void setSlots(Map<EntityType<?>, Map<String, SlotGroupImpl>> slots) {
		this.slots.clear();
		this.slots.putAll(slots);
	}

	public void sync(ServerPlayer playerEntity) {
		playerEntity.connection.send(new ClientboundCustomPayloadPacket(new SyncSlotsPayload(Map.copyOf(this.slots))));
	}

	public void sync(List<? extends ServerPlayer> players) {
		var packet = new ClientboundCustomPayloadPacket(new SyncSlotsPayload(Map.copyOf(this.slots)));

		for(ServerPlayer player : players) {

			((TrinketInventoryMenu) player.inventoryMenu).trinkets$updateTrinketSlots(true);
			var trinkets = TrinketsApi.getAttachment(player);
			Map<String, Integer> tag = new HashMap<>();
			((LivingEntityTrinketAttachment) trinkets).inventory.forEach((key, v) -> {
				tag.put(key, v.getContainerSize());
			});
			player.connection.send(new ClientboundCustomPayloadPacket(new SyncInventoryPayload(player.getId(), Map.of(), tag)));
			player.connection.send(packet);
		}
	}
}