package eu.pb4.trinkets.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import eu.pb4.trinkets.impl.TrinketModifiers;
import eu.pb4.trinkets.impl.TrinketSlot;
import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.SlotReference;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.TrinketsAttributeModifiersComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Adds a tooltip for trinkets describing slots and attributes
 *
 * @author Emi
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {


	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/minecraft/world/entity/player/Player;)V", shift = Shift.BEFORE), method = "addDetailsToTooltip")
	private void getTooltip(Item.TooltipContext context, TooltipDisplay displayComponent, Player player, TooltipFlag type, Consumer<Component> textConsumer, CallbackInfo ci) {
		TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
			ItemStack self = (ItemStack) (Object) this;

            boolean showAttributeTooltip = displayComponent.shows(TrinketsAttributeModifiersComponent.TYPE);
			if (!showAttributeTooltip) {
				// nothing to do
				return;
			}

			boolean canEquipAnywhere = true;
			Set<SlotType> slots = Sets.newHashSet();
			Map<SlotType, Multimap<Holder<Attribute>, AttributeModifier>> modifiers = Maps.newHashMap();
			Multimap<Holder<Attribute>, AttributeModifier> defaultModifier = null;
			boolean allModifiersSame = true;
			int slotCount = 0;

			for (Map.Entry<String, Map<String, TrinketInventory>> group : comp.getInventory().entrySet()) {
				outer:
				for (Map.Entry<String, TrinketInventory> inventory : group.getValue().entrySet()) {
					TrinketInventory trinketInventory = inventory.getValue();
					SlotType slotType = trinketInventory.getSlotType();
					slotCount++;
					boolean anywhereButHidden = false;
					for (int i = 0; i < trinketInventory.getContainerSize(); i++) {
						SlotReference ref = new SlotReference(trinketInventory, i);
						boolean res = TrinketsApi.evaluatePredicateSet(slotType.getTooltipPredicates(), self, ref, player);
						boolean canInsert = TrinketSlot.canInsert(self, ref, player);
						if (res && canInsert) {
							boolean sameTranslationExists = false;
							for (SlotType t : slots) {
								if (t.getTranslation().getString().equals(slotType.getTranslation().getString())) {
									sameTranslationExists = true;
									break;
								}
							}
							if (!sameTranslationExists) {
								slots.add(slotType);
							}
							Multimap<Holder<Attribute>, AttributeModifier> map = TrinketModifiers.get(self, ref, player);

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
						} else if (canInsert) {
							anywhereButHidden = true;
						}
					}
					if (!anywhereButHidden) {
						canEquipAnywhere = false;
					}
				}
			}

			if (canEquipAnywhere && slotCount > 1) {
				textConsumer.accept(Component.translatable("trinkets.tooltip.slots.any").withStyle(ChatFormatting.GRAY));
			} else if (slots.size() > 1) {
				textConsumer.accept(Component.translatable("trinkets.tooltip.slots.list").withStyle(ChatFormatting.GRAY));
				for (SlotType slotType : slots) {
					textConsumer.accept(slotType.getTranslation().withStyle(ChatFormatting.BLUE));
				}
			} else if (slots.size() == 1) {
				// Should only run once
				for (SlotType slotType : slots) {
					textConsumer.accept(Component.translatable("trinkets.tooltip.slots.single",
								slotType.getTranslation().withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
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
		});
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
				if (attribute.isBound() && attribute.value() instanceof SlotAttributes.SlotEntityAttribute) {
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