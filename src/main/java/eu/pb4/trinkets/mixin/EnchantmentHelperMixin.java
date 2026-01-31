package eu.pb4.trinkets.mixin;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.TrinketSlotTarget;
import eu.pb4.trinkets.api.TrinketComponent;
import eu.pb4.trinkets.api.TrinketsApi;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows enchantments to work on trinket items when used in global entity context
 *
 * @author Patbox
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

	@Inject(at = @At("TAIL"), method = "runIterationOnEquipment(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V")
	private static void forEachTrinket(LivingEntity entity, EnchantmentHelper.EnchantmentInSlotVisitor contextAwareConsumer, CallbackInfo info) {
		Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(entity);
		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			comp.forEach((ref, stack) -> {
				if (!stack.isEmpty()) {
					ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
					if (enchantments != null && !enchantments.isEmpty()) {
						EnchantedItemInUse context = new EnchantedItemInUse(stack, null, entity, (item) -> {
							TrinketsApi.onTrinketBroken(stack, ref, entity);
						});

						for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
							Holder<Enchantment> registryEntry = entry.getKey();
							List<EquipmentSlotGroup> slots = registryEntry.value().definition().slots();
							Set<String> trinketSlots = ((TrinketSlotTarget) (Object) registryEntry.value().definition()).trinkets$slots();

							if (slots.contains(EquipmentSlotGroup.ANY) || slots.contains(EquipmentSlotGroup.ARMOR) || trinketSlots.contains(ref.inventory().getSlotType().getId())) {
								contextAwareConsumer.accept(registryEntry, entry.getIntValue(), context);
							}
						}
					}
				}
			});
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), method = "getRandomItemWith")
	private static void addTrinketsAsChoices(DataComponentType<?> componentType, LivingEntity entity, Predicate<ItemStack> stackPredicate, CallbackInfoReturnable<Optional<EnchantedItemInUse>> info, @Local List<EnchantedItemInUse> list) {
		Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(entity);
		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			comp.forEach((ref, stack) -> {
				if (stackPredicate.test(stack)) {
					ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
					for(Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
						Holder<Enchantment> registryEntry = entry.getKey();
						List<EquipmentSlotGroup> slots = registryEntry.value().definition().slots();
						Set<String> trinketSlots = ((TrinketSlotTarget) (Object) registryEntry.value().definition()).trinkets$slots();

						if (registryEntry.value().effects().has(componentType)
								&& (slots.contains(EquipmentSlotGroup.ANY) || slots.contains(EquipmentSlotGroup.ARMOR)
								|| trinketSlots.contains(ref.inventory().getSlotType().getId()))
						) {
							list.add(new EnchantedItemInUse(stack, null, entity, (item) -> {
								TrinketsApi.onTrinketBroken(stack, ref, entity);
							}));
						}
					}
				}
			});
		}
	}
}