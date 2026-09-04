package eu.pb4.trinkets.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.ModInitializer;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.impl.data.EntitySlotLoader;
import eu.pb4.trinkets.impl.data.SlotLoader;
import eu.pb4.trinkets.impl.payload.*;
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
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.BitSet;
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

    public static void syncConfigChanges(MinecraftServer server) {
        var p = new ClientboundCustomPayloadPacket(new SyncConfigPayload(TrinketsConfig.instance.gameplay));
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity livingEntity) {
                    LivingEntityTrinketAttachment.get(livingEntity).rebuild();
                }
            }
        }

        for (var player : server.getPlayerList().getPlayers()) {
            player.connection.send(p);
            syncSlots(player, true);
        }
    }

    public static void syncSlots(ServerPlayer player, boolean reinitialize) {
        ((TrinketInventoryMenu) player.inventoryMenu).trinkets$updateTrinketSlots(reinitialize);
        var trinkets = TrinketsApi.getAttachment(player);
        Map<String, Integer> tag = new HashMap<>();
        var hidden = new HashMap<String, BitSet>();
        ((LivingEntityTrinketAttachment) trinkets).inventory.forEach((id, v) -> {
            tag.put(id, v.getContainerSize());
            hidden.put(id, v.copyHiddenSlots());
        });
        player.connection.send(new ClientboundCustomPayloadPacket(new SyncInventoryPayload(player.getId(), Map.of(), tag, hidden)));
    }

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
        CommonAbstraction.INSTANCE.registerClientboundPlayPayload(TrinketsNetwork.SYNC_CONFIG, SyncConfigPayload.CODEC.cast());

        CommonAbstraction.INSTANCE.registerServerboundPlayPayload(TrinketsNetwork.TOGGLE_VISIBILITY, ToggleVisibilityPayload.CODEC, (player, payload) -> {
            if (!TrinketsConfig.instance.gameplay.equipmentHiding) {
                return;
            }
            var slot = TrinketsApi.getAttachment(player).getSlotAccess(payload.reference());
            if (slot != null && slot.inventory() instanceof TrinketInventoryImpl inventory) {
                inventory.setVisible(slot.index(), payload.value());
            }
        });

        CommonAbstraction.INSTANCE.registerServerboundPlayPayload(TrinketsNetwork.TOGGLE_COSMETIC_MODE, ToggleCosmeticModePayload.CODEC, (player, payload) -> {
            if (!TrinketsConfig.instance.gameplay.cosmeticSlots) {
                return;
            }
            ((TrinketInventoryMenu) player.inventoryMenu).trinkets$setCosmeticMode(payload.value());
        });


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
                                        .then(literal("visiblity")
                                                .then(argument("offset", integer(0))
                                                        .then(argument("show", BoolArgumentType.bool())
                                                                .executes(TrinketsMain::setTrinketSlotVisibility)
                                                        )
                                                )
                                        )
                                        .then(literal("modifier")
                                                .then(literal("add").then(argument("id", IdentifierArgument.id()).then(argument("value", DoubleArgumentType.doubleArg())
                                                        .then(literal("add_value").executes(ctx -> addModifierCommand(ctx, AttributeModifier.Operation.ADD_VALUE)))
                                                        .then(literal("add_multiplied_total").executes(ctx -> addModifierCommand(ctx, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)))
                                                        .then(literal("add_multiplied_base").executes(ctx -> addModifierCommand(ctx, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)))
                                                )))
                                                .then(literal("remove").then(argument("id", IdentifierArgument.id()).executes(TrinketsMain::removeModifierCommand)))
                                                .then(literal("get").then(argument("id", IdentifierArgument.id()).executes(TrinketsMain::getModifierCommand)))
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

        TrinketsApi.registerTrinketPredicate(BuiltInTrinketConditions.ANY_TRINKET, (stack, ref, entity) -> {
            var component = stack.get(TrinketDataComponents.EQUIPMENT);
            return stack.is(DefaultTrinketSlotTags.TRINKETS) || component != null && !component.allowedSlots().isEmpty();
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

    private static int addModifierCommand(CommandContext<CommandSourceStack> context, AttributeModifier.Operation operation) throws CommandSyntaxException {
        String slot = IdentifierArgument.getId(context, "slot").getPath();
        Identifier identifier = IdentifierArgument.getId(context, "id");
        double amount = DoubleArgumentType.getDouble(context, "value");

        if (EntityArgument.getEntity(context, "entity") instanceof LivingEntity livingEntity) {
            TrinketAttachment comp = TrinketsApi.getAttachment(livingEntity);
            var inv = comp.getInventory(slot);
            if (inv != null) {
                inv.addSlotCountModifier(new AttributeModifier(identifier, amount, operation));
                context.getSource().sendSuccess(
                        () -> Component.translatable(
                                "commands.trinkets.modifier.add.success", Component.translationArg(identifier), inv.slotType().getTranslation(), livingEntity.getName()
                        ),
                        false
                );
                return Command.SINGLE_SUCCESS;
            } else {
                context.getSource().sendFailure(Component.translatable("commands.trinkets.inventory_does_not_exist", slot));
            }
        } else {
            context.getSource().sendFailure(Component.translatable("commands.trinkets.not_a_living_entity"));
        }
        return 0;
    }

    private static int removeModifierCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String slot = IdentifierArgument.getId(context, "slot").getPath();
        Identifier identifier = IdentifierArgument.getId(context, "id");

        if (EntityArgument.getEntity(context, "entity") instanceof LivingEntity livingEntity) {
            TrinketAttachment comp = TrinketsApi.getAttachment(livingEntity);
            var inv = comp.getInventory(slot);
            if (inv != null) {
                inv.removeSlotCountModifier(identifier);
                context.getSource().sendSuccess(
                        () -> Component.translatable(
                                "commands.trinkets.modifier.remove.success", Component.translationArg(identifier), inv.slotType().getTranslation(), livingEntity.getName()
                        ),
                        false
                );
                return Command.SINGLE_SUCCESS;
            } else {
                context.getSource().sendFailure(Component.translatable("commands.trinkets.inventory_does_not_exist", slot));
            }
        } else {
            context.getSource().sendFailure(Component.translatable("commands.trinkets.not_a_living_entity"));
        }
        return 0;
    }

    private static int getModifierCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String slot = IdentifierArgument.getId(context, "slot").getPath();
        Identifier identifier = IdentifierArgument.getId(context, "id");

        if (EntityArgument.getEntity(context, "entity") instanceof LivingEntity livingEntity) {
            TrinketAttachment comp = TrinketsApi.getAttachment(livingEntity);
            var inv = comp.getInventory(slot);
            if (inv != null) {
                context.getSource().sendSuccess(
                        () -> Component.translatable(
                                "commands.trinkets.modifier.value.get.success", Component.translationArg(identifier), inv.slotType().getTranslation(), livingEntity.getName(),
                                inv.getSlotCountModifier(identifier) != null ? inv.getSlotCountModifier(identifier).amount() : 0
                        ),
                        false
                );
                return Command.SINGLE_SUCCESS;
            } else {
                context.getSource().sendFailure(Component.translatable("commands.trinkets.inventory_does_not_exist", slot));
            }
        } else {
            context.getSource().sendFailure(Component.translatable("commands.trinkets.not_a_living_entity"));
        }
        return 0;
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
                    context.getSource().sendFailure(Component.translatable("commands.trinkets.slot_does_not_exit", slot, offset));
                }
            } else {
                context.getSource().sendFailure(Component.translatable("commands.trinkets.not_a_living_entity"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static int setTrinketSlotVisibility(CommandContext<CommandSourceStack> context) {
        try {
            String slot = IdentifierArgument.getId(context, "slot").getPath();
            int offset = context.getArgument("offset", Integer.class);
            var show = context.getArgument("show", Boolean.class);
            if (EntityArgument.getEntity(context, "entity") instanceof LivingEntity livingEntity) {
                var comp = LivingEntityTrinketAttachment.get(livingEntity);
                var access = comp.getInventory(slot);
                if (access != null) {
                    access.setVisible(offset, show);

                    if (livingEntity instanceof ServerPlayer player) {
                        player.connection.send(new ClientboundCustomPayloadPacket(new SyncInventoryPayload(player.getId(), Map.of(), Map.of(), Map.of(slot, access.copyHiddenSlots()))));
                    }
                } else {
                    context.getSource().sendFailure(Component.translatable("commands.trinkets.slot_does_not_exit", slot, offset));
                }
            } else {
                context.getSource().sendFailure(Component.translatable("commands.trinkets.not_a_living_entity"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}