package eu.pb4.trinkets.impl;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.logging.LogUtils;
import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.ModInitializer;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.impl.payload.BreakPayload;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import eu.pb4.trinkets.impl.payload.SyncSlotsPayload;
import eu.pb4.trinkets.impl.platform.CommonAbstraction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.data.SlotLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class TrinketsMain implements ModInitializer {

	public static final String NAMESPACE = "trinkets";
	public static final String UNIVERSAL_MOD_ID = "trinkets_updated";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final Map<Item, TrinketCallback> CALLBACKS = new IdentityHashMap<>();
	public static final Map<Identifier, TrinketsApi.TrinketPredicate> PREDICATES = new HashMap<>();
	public static final boolean IS_CLIENT = CommonAbstraction.INSTANCE.isClient();

	@Override
	public void onInitialize(ModContainer modContainer) {
		TrinketsConfig.load();
		CommonAbstraction.get().registerServerReloadListener(SlotLoader.ID, SlotLoader.INSTANCE);
		CommonAbstraction.get().registerServerReloadListener(EntitySlotLoader.ID ,EntitySlotLoader.SERVER, SlotLoader.ID);

		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(NAMESPACE, "attribute_modifiers"), TrinketDataComponents.ATTRIBUTE_MODIFIERS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(NAMESPACE, "equipment"), TrinketDataComponents.EQUIPMENT);

		CommonAbstraction.get().registerClientboundPlayPayload(TrinketsNetwork.BREAK, BreakPayload.CODEC);
		CommonAbstraction.get().registerClientboundPlayPayload(TrinketsNetwork.SYNC_INVENTORY, SyncInventoryPayload.CODEC);
		CommonAbstraction.get().registerClientboundPlayPayload(TrinketsNetwork.SYNC_SLOTS, SyncSlotsPayload.CODEC);

		CommonAbstraction.get().registerCommand((dispatcher, registry) ->
			dispatcher.register(literal("trinkets")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(
					literal("set")
					.then(
						argument("group", string())
						.then(
							argument("slot", string())
							.then(
								argument("offset", integer(0))
								.then(
									argument("stack", ItemArgument.item(registry))
									.executes(context -> {
										try {
										return trinketsCommand(context, 1);

										} catch (Exception e) {
											e.printStackTrace();
											return -1;
										}
									})
									.then(
										argument("count", integer(1))
										.executes(context -> {
											int amount = context.getArgument("amount", Integer.class);
											return trinketsCommand(context, amount);
										})
									)
								)
							)
						)
					)
				)
				.then(
					literal("clear")
					.executes(context -> {
						try {
							return clearCommand(context);
						} catch (Exception e){
							e.printStackTrace();
							return -1;
						}
					})
				)
			));


		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "all"), (stack, ref, entity) -> true);
		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "none"), (stack, ref, entity) -> false);
		TagKey<Item> trinketsAll = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", "all"));

		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "default"), (stack, ref, entity) -> {
			SlotType slot = ref.inventory().slotType();
			TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", slot.getId()));
			var component = stack.get(TrinketDataComponents.EQUIPMENT);

			if (stack.is(tag) || stack.is(trinketsAll) || component != null && component.allowedSlots().contains(slot.getId())) {
				return true;
			}
			return false;
		});

		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "tag"), (stack, ref, entity) -> {
			SlotType slot = ref.inventory().slotType();
			TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", slot.getId()));

			if (stack.is(tag) || stack.is(trinketsAll)) {
				return true;
			}
			return false;
		});

		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "component"), (stack, ref, entity) -> {
			SlotType slot = ref.inventory().slotType();
			var component = stack.get(TrinketDataComponents.EQUIPMENT);

			if (component != null && component.allowedSlots().contains(slot.getId())) {
				return true;
			}
			return false;
		});

		TrinketsApi.registerTrinketPredicate(Identifier.fromNamespaceAndPath("trinkets", "attributes"), (stack, ref, entity) -> {
			var b = new MutableBoolean();

			TrinketUtilities.forEachModifier(entity, stack, ref, (_, _) -> b.setTrue());

			return b.booleanValue();
		});

		CommonAbstraction.get().registerMobConversion(LivingEntityTrinketAttachment::copyData);
	}

	private static int clearCommand(CommandContext<CommandSourceStack> context){
		if (context.getSource().getEntity() instanceof LivingEntity livingEntity) {
			LivingEntityTrinketAttachment.get(livingEntity).clearContents();
		}
		return 1;
	}

	private static int trinketsCommand(CommandContext<CommandSourceStack> context, int amount) {
		try {
			String group = context.getArgument("group", String.class);
			String slot = context.getArgument("slot", String.class);
			int offset = context.getArgument("offset", Integer.class);
			ItemInput stack = context.getArgument("stack", ItemInput.class);
			var entity = context.getSource().getEntity();
			if (entity instanceof LivingEntity livingEntity) {
				TrinketAttachment comp = TrinketsApi.getAttachment(livingEntity);
				SlotGroup slotGroup = comp.getGroups().getOrDefault(group, null);
				if (slotGroup != null) {
					SlotType slotType = slotGroup.slots().getOrDefault(slot, null);
					if (slotType != null) {
						if (offset >= 0 && offset < slotType.amount()) {
							comp.getInventory(group + '/' + slot).setItem(offset, stack.createItemStack(amount));
							return Command.SINGLE_SUCCESS;
						} else {
							context.getSource().sendFailure(Component.literal(offset + " offset does not exist for slot"));
						}
					} else {
						context.getSource().sendFailure(Component.literal(slot + " does not exist"));
					}
				} else {
					context.getSource().sendFailure(Component.literal(group + " does not exist"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
}