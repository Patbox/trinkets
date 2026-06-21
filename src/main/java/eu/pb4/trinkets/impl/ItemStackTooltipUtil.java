package eu.pb4.trinkets.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Adds a tooltip for trinkets describing slots and attributes
 *
 * @author Emi
 */
public class ItemStackTooltipUtil {
    public static void getTooltip(ItemStack self, TooltipDisplay displayComponent, Player player, Consumer<Component> textConsumer) {
        if (player == null || !TrinketsConfig.instance.showItemTooltip) return;

        var comp = LivingEntityTrinketAttachment.get(player);

        boolean showAttributeTooltip = displayComponent.shows(TrinketDataComponents.ATTRIBUTE_MODIFIERS);
        if (!showAttributeTooltip) {
            // nothing to do
            return;
        }

        boolean canEquipAnywhere = true;
        List<Tuple<SlotType, Boolean>> slots = new ArrayList<>();
        Map<SlotType, Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>>> modifiers = Maps.newHashMap();
        Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> defaultModifier = null;
        boolean allModifiersSame = true;
        int slotCount = 0;


        for (var trinketInventory : comp.inventory.values()) {
            SlotType slotType = trinketInventory.slotType();
            slotCount++;
            for (int i = 0; i < trinketInventory.getContainerSize(); i++) {
                var ref = trinketInventory.getOrCreateSlotAccess(i);

                var res = slotType.tooltipCheck(self, ref, player);
                var isValidForSlot = TrinketSlot.isSlotCompatible(self, ref, player);
                var canInsert = isValidForSlot && TrinketSlot.isEquipable(self, ref, player);

                if (res && isValidForSlot) {
                    boolean sameTranslationExists = false;
                    for (var t : slots) {
                        if (t.getA().getTranslation().getString().equals(slotType.getTranslation().getString())) {
                            sameTranslationExists = true;
                            if (canInsert && !t.getB()) {
                                t.setB(true);
                            }
                            break;
                        }
                    }

                    if (!sameTranslationExists) {
                        slots.add(new Tuple<>(slotType, canInsert));
                    }
                    Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> map = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
                    TrinketUtilities.forEachModifier(player, self, ref, (atr, mod, dis) -> map.put(atr, new Tuple<>(mod, dis)));

                    if (defaultModifier == null) {
                        defaultModifier = map;
                    } else if (allModifiersSame) {
                        allModifiersSame = areMapsEqual(defaultModifier, map);
                    }

                    boolean duplicate = false;
                    for (var entry : modifiers.entrySet()) {
                        if (entry.getKey().getTranslation().getString().equals(slotType.getTranslation().getString())) {
                            if (areMapsEqual(entry.getValue(), map)) {
                                duplicate = true;
                                break;
                            }
                        }
                    }

                    if (!duplicate) {
                        modifiers.put(slotType, map);
                    }
                    break;
                } else {
                    canEquipAnywhere = false;
                }
            }
        }


        if (canEquipAnywhere && slotCount > 1) {
            textConsumer.accept(Component.translatable("trinkets.tooltip.slots.any").withStyle(ChatFormatting.GRAY));
        } else if (slots.size() > 1) {
            textConsumer.accept(Component.translatable("trinkets.tooltip.slots.list").withStyle(ChatFormatting.GRAY));
            if (slots.size() > 6) {
                var t = System.currentTimeMillis() / 800;

                for (int i = 0; i < 6; i++) {
                    var slotType = slots.get((int) ((t + i) % slots.size()));
                    textConsumer.accept(slotType.getA().getTranslation().withStyle(slotType.getB() ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY));
                }
            } else {
                for (var slotType : slots) {
                    textConsumer.accept(slotType.getA().getTranslation().withStyle(slotType.getB() ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY));
                }
            }
        } else if (slots.size() == 1) {
            // Should only run once
            for (var slotType : slots) {
                textConsumer.accept(Component.translatable("trinkets.tooltip.slots.single",
                        slotType.getA().getTranslation().withStyle(slotType.getB() ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY)
                ).withStyle(ChatFormatting.GRAY));
            }
        }

        if (!modifiers.isEmpty() && showAttributeTooltip) {
            if (allModifiersSame) {
                if (defaultModifier != null && !defaultModifier.isEmpty()) {
                    textConsumer.accept(Component.translatable("trinkets.tooltip.attributes.all").withStyle(ChatFormatting.GRAY));
                    addAttributes(textConsumer, defaultModifier);
                }
            } else {
                for (var entry : modifiers.entrySet()) {
                    textConsumer.accept(Component.translatable("trinkets.tooltip.attributes.single",
                            entry.getKey().getTranslation().withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
                    addAttributes(textConsumer, entry.getValue());
                }
            }
        }
    }

    private static void addAttributes(Consumer<Component> textConsumer, Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> map) {
        if (!map.isEmpty()) {
            for (var entry : map.entries()) {
                Holder<Attribute> attribute = entry.getKey();
                var tuple = entry.getValue();
                if (tuple.getB().type() == ItemAttributeModifiers.Display.Type.HIDDEN) {
                    continue;
                }

                AttributeModifier modifier = tuple.getA();
                double g = modifier.amount();

                if (modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE && modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                        g *= 10.0D;
                    }
                } else {
                    g *= 100.0D;
                }

                if (tuple.getB() instanceof ItemAttributeModifiers.Display.OverrideText(var t)) {
                    textConsumer.accept(t);
                    continue;
                }


                var text = Component.translatable(attribute.value().getDescriptionId());
                if (attribute.isBound() && attribute.value() instanceof SlotAttributes.SlotModifyingAttribute) {
                    text = Component.translatable("trinkets.tooltip.attributes.slots", text);
                }


                ChatFormatting color = attribute.value().getStyle(g > 0.0D);
                
                if (g > 0.0D) {
                    textConsumer.accept(Component.translatable("attribute.modifier.plus." + modifier.operation().id(),
                            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(g), text).withStyle(color));
                } else if (g < 0.0D) {
                    g *= -1.0D;
                    textConsumer.accept(Component.translatable("attribute.modifier.take." + modifier.operation().id(),
                            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(g), text).withStyle(color));
                }
            }
        }
    }

    // `equals` doesn't test thoroughly
    private static boolean areMapsEqual(Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> map1, Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> map2) {
        if (map1.size() != map2.size()) {
            return false;
        } else {
            for (Holder<Attribute> attribute : map1.keySet()) {
                if (!map2.containsKey(attribute)) {
                    return false;
                }

                var col1 = map1.get(attribute);
                var col2 = map2.get(attribute);

                if (col1.size() != col2.size()) {
                    return false;
                } else {
                    var iter = col2.iterator();

                    for (var modifier : col1) {
                        AttributeModifier eam = iter.next().getA();

                        //we can't check identifiers. EAMs will have slot-specific identifiers so fail total equality by nature.
                        if (!modifier.getA().operation().equals(eam.operation())) {
                            return false;
                        }
                        if (modifier.getA().amount() != eam.amount()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
