package eu.pb4.trinkets.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.ModInitializer;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.data.SlotLoader;
import eu.pb4.trinkets.impl.payload.BreakPayload;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import eu.pb4.trinkets.impl.payload.SyncSlotsPayload;
import eu.pb4.trinkets.impl.platform.CommonAbstraction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

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
        CommonAbstraction.INSTANCE.registerServerReloadListener(SlotLoader.ID, SlotLoader.INSTANCE);
        CommonAbstraction.INSTANCE.registerServerReloadListener(EntitySlotLoader.ID, EntitySlotLoader.SERVER, SlotLoader.ID);

        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(NAMESPACE, "attribute_modifiers"), TrinketDataComponents.ATTRIBUTE_MODIFIERS);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(NAMESPACE, "equipment"), TrinketDataComponents.EQUIPMENT);

        CommonAbstraction.INSTANCE.registerClientboundPlayPayload(TrinketsNetwork.BREAK, BreakPayload.CODEC);
        CommonAbstraction.INSTANCE.registerClientboundPlayPayload(TrinketsNetwork.SYNC_INVENTORY, SyncInventoryPayload.CODEC);
        CommonAbstraction.INSTANCE.registerClientboundPlayPayload(TrinketsNetwork.SYNC_SLOTS, SyncSlotsPayload.CODEC);

        CommonAbstraction.INSTANCE.registerCommand((dispatcher, registry) ->
                dispatcher.register(literal("trinkets")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(argument("entity", EntityArgument.entity())
                                .then(argument("slot", IdentifierArgument.id())
                                        .suggests((commandContext, suggestionsBuilder) -> {
                                            try {
                                                var entity = EntityArgument.getEntity(commandContext, "entity");

                                                for (var group : SlotGroup.getEntityGroups(entity).entrySet()) {
                                                    for (var slot : group.getValue().getSlots()) {
                                                        var id = slot.getId();
                                                        if (id.contains(suggestionsBuilder.getRemainingLowerCase())) {
                                                            suggestionsBuilder.suggest(id, slot.getTranslation());
                                                        }
                                                    }
                                                }
                                            } catch (Throwable ignored) {
                                            }
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .then(literal("set")
                                                .then(argument("offset", integer(0))
                                                        .then(argument("stack", ItemArgument.item(registry))
                                                                .executes(context -> {
                                                                    try {
                                                                        return setTrinketSlotCommand(context, 1);

                                                                    } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                        return -1;
                                                                    }
                                                                })
                                                                .then(argument("count", integer(1))
                                                                        .executes(context -> {
                                                                            int amount = context.getArgument("count", Integer.class);
                                                                            return setTrinketSlotCommand(context, amount);
                                                                        }))
                                                        )
                                                )
                                        )
                                )
                        )

                ));


        TrinketsApi.registerTrinketPredicate(BuiltInTrinketConditions.ALL, (stack, ref, entity) -> true);
        TrinketsApi.registerTrinketPredicate(BuiltInTrinketConditions.NONE, (stack, ref, entity) -> false);

        TrinketsApi.registerTrinketPredicate(BuiltInTrinketConditions.DEFAULT, (stack, ref, entity) -> {
            SlotType slot = ref.inventory().slotType();
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", slot.getId()));
            var component = stack.get(TrinketDataComponents.EQUIPMENT);

            return stack.is(tag) || stack.is(DefaultTrinketSlotTags.ALL) || component != null && (component.allowedSlots().contains(slot.getId()) || component.allowedSlots().contains("any"));
        });

        TrinketsApi.registerTrinketPredicate(BuiltInTrinketConditions.TAG, (stack, ref, entity) -> {
            SlotType slot = ref.inventory().slotType();
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", slot.getId()));

            return stack.is(tag) || stack.is(DefaultTrinketSlotTags.ALL);
        });

        TrinketsApi.registerTrinketPredicate(BuiltInTrinketConditions.COMPONENT, (stack, ref, entity) -> {
            SlotType slot = ref.inventory().slotType();
            var component = stack.get(TrinketDataComponents.EQUIPMENT);

            return component != null && (component.allowedSlots().contains(slot.getId()) || component.allowedSlots().contains("any"));
        });

        TrinketsApi.registerTrinketPredicate(BuiltInTrinketConditions.ATTRIBUTES, (stack, ref, entity) -> {
            var b = new MutableBoolean();

            TrinketUtilities.forEachModifier(entity, stack, ref, (_, _) -> b.setTrue());

            return b.booleanValue();
        });

        CommonAbstraction.INSTANCE.registerMobConversion(LivingEntityTrinketAttachment::copyData);
    }

    private static int setTrinketSlotCommand(CommandContext<CommandSourceStack> context, int amount) {
        try {
            String slot = IdentifierArgument.getId(context, "slot").getPath();
            int offset = context.getArgument("offset", Integer.class);
            ItemInput stack = context.getArgument("stack", ItemInput.class);
            if (EntityArgument.getEntity(context, "entity") instanceof LivingEntity livingEntity) {
                TrinketAttachment comp = TrinketsApi.getAttachment(livingEntity);
                var access = comp.getSlotAccess(slot, offset);
                if (access != null && access.set(stack.createItemStack(amount))) {
                    return Command.SINGLE_SUCCESS;
                } else {
                    context.getSource().sendFailure(Component.literal("Slot '" + slot + "' at " + offset + " offset does not exist!"));
                }
            } else {
                context.getSource().sendFailure(Component.literal("Not a living entity!"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}