package eu.pb4.trinkets.impl;

import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.api.component.TrinketsAttributeModifiersComponent;
import eu.pb4.trinkets.api.event.TrinketEquipCallback;
import eu.pb4.trinkets.api.event.TrinketEquipmentAttributeModifiersCallback;
import eu.pb4.trinkets.api.event.TrinketEquipmentChangedCallback;
import eu.pb4.trinkets.api.event.TrinketUnequipCallback;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import eu.pb4.trinkets.impl.slots.TrinketSlotTarget;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.gameevent.GameEvent;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class TrinketUtilities {
    public static void callTrinketEquipmentChange(ItemStack previous, ItemStack newStack, TrinketSlotAccess slot, LivingEntity entity) {
        if (previous.is(newStack.getItem())) {
            TrinketEquipmentChangedCallback.EVENT.invoker().onEquipmentChanged(previous, newStack, slot, entity);
            TrinketCallback.getCallback(previous).onEquippedStackChanged(previous, newStack, slot, entity);
        } else {
            TrinketCallback.getCallback(previous).onUnequip(previous, slot, entity);
            TrinketUnequipCallback.EVENT.invoker().onUnequip(previous, slot, entity);

            TrinketEquipmentChangedCallback.EVENT.invoker().onEquipmentChanged(previous, newStack, slot, entity);
            TrinketCallback.getCallback(previous).onEquippedStackChanged(previous, newStack, slot, entity);
            TrinketCallback.getCallback(newStack).onEquippedStackChanged(previous, newStack, slot, entity);

            TrinketCallback.getCallback(newStack).onEquip(newStack, slot, entity);
            TrinketEquipCallback.EVENT.invoker().onEquip(newStack, slot, entity);
        }
    }

    public static void forEachModifier(LivingEntity entity, ItemStack stack, TrinketSlotAccess slot,
                                       final BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        var modifiers = stack.getOrDefault(TrinketDataComponents.ATTRIBUTE_MODIFIERS, TrinketsAttributeModifiersComponent.DEFAULT);
        modifiers.forEach(slot, consumer);

        TrinketCallback.getCallback(stack).forEachTrinketModifier(stack, slot, entity, SlotAttributes.getIdentifier(slot), consumer);
        TrinketEquipmentAttributeModifiersCallback.EVENT.invoker().forEachTrinketModifier(stack, slot, entity, SlotAttributes.getIdentifier(slot), consumer);

        EnchantmentHelper.runIterationOnItem(stack, (enchantment, level) -> enchantment.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach((effect) -> {
            if (isEnchantmentTrinketCompatible(enchantment, slot)) {
                consumer.accept(effect.attribute(), effect.getModifier(level, slot::getAsIdentifierPath));
            }
        }));
    }

    public static void forEachModifier(LivingEntity entity, ItemStack stack, TrinketSlotAccess slot,
                                       final TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer) {
        var modifiers = stack.getOrDefault(TrinketDataComponents.ATTRIBUTE_MODIFIERS, TrinketsAttributeModifiersComponent.DEFAULT);
        modifiers.forEach(slot, consumer);

        TrinketCallback.getCallback(stack).forEachTrinketModifier(stack, slot, entity, SlotAttributes.getIdentifier(slot), (a, b) -> consumer.accept(a, b, ItemAttributeModifiers.Display.attributeModifiers()));
        TrinketEquipmentAttributeModifiersCallback.EVENT.invoker().forEachTrinketModifier(stack, slot, entity, SlotAttributes.getIdentifier(slot), (a, b) -> consumer.accept(a, b, ItemAttributeModifiers.Display.attributeModifiers()));

        EnchantmentHelper.runIterationOnItem(stack, (enchantment, level) -> enchantment.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach((effect) -> {
            if (isEnchantmentTrinketCompatible(enchantment, slot)) {
                consumer.accept(effect.attribute(), effect.getModifier(level, slot::getAsIdentifierPath), ItemAttributeModifiers.Display.attributeModifiers());
            }
        }));
    }

    public static boolean isEnchantmentTrinketCompatible(Holder<Enchantment> registryEntry, TrinketSlotAccess ref) {
        List<EquipmentSlotGroup> slots = registryEntry.value().definition().slots();
        Set<String> trinketSlots = ((TrinketSlotTarget) (Object) registryEntry.value().definition()).trinkets$slots();

        return slots.contains(EquipmentSlotGroup.ANY) || slots.contains(EquipmentSlotGroup.ARMOR) || trinketSlots.contains(ref.inventory().slotType().getId());
    }

    public static void runIterationOnItem(ItemStack piece, TrinketSlotAccess slot, LivingEntity owner, EnchantmentHelper.EnchantmentInSlotVisitor consumer) {
        if (!piece.isEmpty()) {
            ItemEnchantments itemEnchantments = piece.get(DataComponents.ENCHANTMENTS);
            if (itemEnchantments != null && !itemEnchantments.isEmpty()) {
                EnchantedItemInUse itemInUse = new EnchantedItemInUse(piece, EquipmentSlot.BODY, owner);

                for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantments.entrySet()) {
                    Holder<Enchantment> enchantment = entry.getKey();
                    if (isEnchantmentTrinketCompatible(enchantment, slot)) {
                        consumer.accept(enchantment, entry.getIntValue(), itemInUse);
                    }
                }

            }
        }
    }

    public static InteractionResult swapWithEquipmentSlot(final ItemStack inHand, final Player user) {
        var equipment = inHand.get(TrinketDataComponents.EQUIPMENT);

        if (equipment == null || equipment.canBeEquippedBy(user)) {
            var comp = LivingEntityTrinketAttachment.get(user);

            for (var inv : comp.inventory.values()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (inv.getItem(i).isEmpty()) {
                        var ref = inv.getSlotAccess(i);
                        if (TrinketSlot.canInsert(inHand, ref, user) && inv.slotType().interactEquipableCheck(inHand, ref, user)) {
                            ItemStack newStack = inHand.copy();
                            inv.setItem(i, newStack);
                            TrinketUtilities.playEquipmentSound(newStack, ref, user);
                            inHand.shrink(1);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }

            if (equipment == null || equipment.swappable()) {
                for (var inv : comp.inventory.values()) {
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        var current = inv.getItem(i);
                        var ref = inv.getSlotAccess(i);
                        if (TrinketSlot.mayPickup(current, ref, user) && TrinketSlot.canInsert(inHand, ref, user) && inv.slotType().interactEquipableCheck(inHand, ref, user)) {
                            TrinketUtilities.playEquipmentSound(inHand, ref, user);
                            if (inHand.getCount() <= 1) {
                                ItemStack swappedToHand = current.isEmpty() ? inHand : current.copyAndClear();
                                ItemStack swappedToEquipment = user.isCreative() ? inHand.copy() : inHand.copyAndClear();
                                inv.setItem(i, swappedToEquipment);
                                return InteractionResult.SUCCESS.heldItemTransformedTo(swappedToHand);
                            } else {
                                ItemStack swappedToInventory = current.copyAndClear();
                                ItemStack swappedToEquipment = inHand.consumeAndReturn(1, user);
                                inv.setItem(i, swappedToEquipment);
                                if (!user.getInventory().add(swappedToInventory)) {
                                    user.drop(swappedToInventory, false);
                                }
                                return InteractionResult.SUCCESS.heldItemTransformedTo(inHand);
                            }
                        }
                    }
                }
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public static void playEquipmentSound(ItemStack stack, TrinketSlotAccess ref, LivingEntity user) {
        var trinket = TrinketCallback.getCallback(stack);
        var soundEvent = trinket.getEquipSound(stack, ref, user);
        if (!stack.isEmpty() && soundEvent != null) {
            user.gameEvent(GameEvent.EQUIP);
            user.playSound(soundEvent.value(), 1.0F, 1.0F);
        }
    }

    public static boolean hasOneOfSlots(LivingEntity entity, List<String> slots) {
        var ent = SlotType.getEntitySlots(entity);
        for (var slot : slots) {
            if (slot.equals("any") && !ent.isEmpty()) {
                return true;
            }

            if (ent.containsKey(slot)) {
                return true;
            }
        }

        return false;
    }
}
