package eu.pb4.trinkets.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketUtilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Consumer;

/**
 * Adds a tooltip for trinkets describing slots and attributes
 *
 * @author Emi
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/minecraft/world/entity/player/Player;)V",
            shift = Shift.BEFORE), require = 0)
    private void getTooltipVanilla(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> builder, CallbackInfo ci) {
        getTooltip(display, player, builder);
    }

    @SuppressWarnings("MixinAnnotationTarget")
    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/util/AttributeUtil;addAttributeTooltips(Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/neoforged/neoforge/common/util/AttributeTooltipContext;)V",
            shift = Shift.BEFORE), require = 0)
    private void getTooltipNeoForge(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> builder, CallbackInfo ci) {
        getTooltip(display, player, builder);
    }


    @Unique
    private void getTooltip(TooltipDisplay displayComponent, Player player, Consumer<Component> textConsumer) {
        if (player == null) return;

        var comp = LivingEntityTrinketAttachment.get(player);

        ItemStack self = (ItemStack) (Object) this;

        boolean showAttributeTooltip = displayComponent.shows(TrinketDataComponents.ATTRIBUTE_MODIFIERS);
        if (!showAttributeTooltip) {
            // nothing to do
            return;
        }

        boolean canEquipAnywhere = true;
        List<Tuple<SlotType, Boolean>> slots = new ArrayList<>();
        Map<SlotType, Multimap<Holder<Attribute>, AttributeModifier>> modifiers = Maps.newHashMap();
        Multimap<Holder<Attribute>, AttributeModifier> defaultModifier = null;
        boolean allModifiersSame = true;
        int slotCount = 0;

        for (var group : comp.getInventory().entrySet()) {
            outer:
            for (var inventory : group.getValue().entrySet()) {
                var trinketInventory = inventory.getValue();
                SlotType slotType = trinketInventory.slotType();
                slotCount++;
                for (int i = 0; i < trinketInventory.getContainerSize(); i++) {
                    var callback = TrinketCallback.getCallback(self);
                    var ref = trinketInventory.getOrCreateSlotAccess(i);

                    var res = slotType.tooltipCheck(self, ref, player);
                    var isValidForSlot = ref.slotType().validatorCheck(self, ref, player);
                    var canInsert = isValidForSlot && callback.canEquip(self, ref, player);

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
                        Multimap<Holder<Attribute>, AttributeModifier> map = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
                        TrinketUtilities.forEachModifier(player, self, ref, map::put);

                        if (defaultModifier == null) {
                            defaultModifier = map;
                        } else if (allModifiersSame) {
                            allModifiersSame = areMapsEqual(defaultModifier, map);
                        }

                        boolean duplicate = false;
                        for (Map.Entry<SlotType, Multimap<Holder<Attribute>, AttributeModifier>> entry : modifiers.entrySet()) {
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
                        continue outer;
                    } else {
                        canEquipAnywhere = false;
                    }
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
                for (Map.Entry<SlotType, Multimap<Holder<Attribute>, AttributeModifier>> entry : modifiers.entrySet()) {
                    textConsumer.accept(Component.translatable("trinkets.tooltip.attributes.single",
                            entry.getKey().getTranslation().withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
                    addAttributes(textConsumer, entry.getValue());
                }
            }
        }
    }

    @Unique
    private void addAttributes(Consumer<Component> textConsumer, Multimap<Holder<Attribute>, AttributeModifier> map) {
        if (!map.isEmpty()) {
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : map.entries()) {
                Holder<Attribute> attribute = entry.getKey();
                AttributeModifier modifier = entry.getValue();
                double g = modifier.amount();

                if (modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE && modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                        g *= 10.0D;
                    }
                } else {
                    g *= 100.0D;
                }

                Component text = Component.translatable(attribute.value().getDescriptionId());
                if (attribute.isBound() && attribute.value() instanceof SlotAttributes.SlotModifyingAttribute) {
                    text = Component.translatable("trinkets.tooltip.attributes.slots", text);
                }
                if (g > 0.0D) {
                    textConsumer.accept(Component.translatable("attribute.modifier.plus." + modifier.operation().id(),
                            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(g), text).withStyle(ChatFormatting.BLUE));
                } else if (g < 0.0D) {
                    g *= -1.0D;
                    textConsumer.accept(Component.translatable("attribute.modifier.take." + modifier.operation().id(),
                            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(g), text).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    // `equals` doesn't test thoroughly
    @Unique
    private boolean areMapsEqual(Multimap<Holder<Attribute>, AttributeModifier> map1, Multimap<Holder<Attribute>, AttributeModifier> map2) {
        if (map1.size() != map2.size()) {
            return false;
        } else {
            for (Holder<Attribute> attribute : map1.keySet()) {
                if (!map2.containsKey(attribute)) {
                    return false;
                }

                Collection<AttributeModifier> col1 = map1.get(attribute);
                Collection<AttributeModifier> col2 = map2.get(attribute);

                if (col1.size() != col2.size()) {
                    return false;
                } else {
                    Iterator<AttributeModifier> iter = col2.iterator();

                    for (AttributeModifier modifier : col1) {
                        AttributeModifier eam = iter.next();

                        //we can't check identifiers. EAMs will have slot-specific identifiers so fail total equality by nature.
                        if (!modifier.operation().equals(eam.operation())) {
                            return false;
                        }
                        if (modifier.amount() != eam.amount()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}