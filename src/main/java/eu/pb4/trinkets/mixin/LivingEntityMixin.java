package eu.pb4.trinkets.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import eu.pb4.trinkets.impl.TrinketModifiers;
import eu.pb4.trinkets.api.SlotReference;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketComponent;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.TrinketSaveData;
import eu.pb4.trinkets.api.event.TrinketEquipCallback;
import eu.pb4.trinkets.api.event.TrinketUnequipCallback;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.api.SlotAttributes.SlotEntityAttribute;
import eu.pb4.trinkets.api.TrinketEnums.DropRule;
import eu.pb4.trinkets.api.event.TrinketDropCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Trinket dropping on death, trinket EAMs, and trinket equip/unequip calls
 *
 * @author Emi
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Unique
	private final Map<String, ItemStack> lastEquippedTrinkets = new HashMap<>();

	@Shadow
	public abstract AttributeMap getAttributes();

	private LivingEntityMixin() {
		super(null, null);
	}

	@Inject(at = @At("HEAD"), method = "canFreeze", cancellable = true)
	private void canFreeze(CallbackInfoReturnable<Boolean> cir) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent((LivingEntity) (Object) this);
		if (component.isPresent()) {
			for (Tuple<SlotReference, ItemStack> equipped : component.get().getAllEquipped()) {
				if (equipped.getB().is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
					cir.setReturnValue(false);
					break;
				}
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "dropEquipment")
	private void dropInventory(ServerLevel world, CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;

		boolean keepInv = world.getGameRules().get(GameRules.KEEP_INVENTORY);
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> trinkets.forEach((ref, stack) -> {
			if (stack.isEmpty()) {
				return;
			}

			DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, entity);

			dropRule = TrinketDropCallback.EVENT.invoker().drop(dropRule, stack, ref, entity);

			TrinketInventory inventory = ref.inventory();

			if (dropRule == DropRule.DEFAULT) {
				dropRule = inventory.getSlotType().getDropRule();
			}

			if (dropRule == DropRule.DEFAULT) {
				if (keepInv && entity.getType() == EntityType.PLAYER) {
					dropRule = DropRule.KEEP;
				} else {
					if (EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
						dropRule = DropRule.DESTROY;
					} else {
						dropRule = DropRule.DROP;
					}
				}
			}

			switch (dropRule) {
				case DROP:
					dropFromEntity(stack);
					// Fallthrough
				case DESTROY:
					inventory.setItem(ref.index(), ItemStack.EMPTY);
					break;
				default:
					break;
			}
		}));
	}

	@Unique
	private void dropFromEntity(ItemStack stack) {
		// Mimic player drop behavior for only players
		if (((Entity) this) instanceof Player player) {
			ItemEntity entity = player.drop(stack, true, false);
		} else if (this.level() instanceof ServerLevel serverWorld) {
			ItemEntity entity = spawnAtLocation(serverWorld, stack);
		}
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity.isRemoved()) {
			return;
		}
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> {
			Map<String, ItemStack> newlyEquippedTrinkets = new HashMap<>();
			Map<String, ItemStack> contentUpdates = new HashMap<>();
			trinkets.forEach((ref, stack) -> {
				TrinketInventory inventory = ref.inventory();
				SlotType slotType = inventory.getSlotType();
				int index = ref.index();
				ItemStack oldStack = getOldStack(slotType, index);
				ItemStack newStack = inventory.getItem(index);
				ItemStack newStackCopy = newStack.copy();
				String newRef = slotType.getGroup() + "/" + slotType.getName() + "/" + index;

				if (!ItemStack.matches(newStack, oldStack)) {

					TrinketsApi.getTrinket(oldStack.getItem()).onUnequip(oldStack, ref, entity);
					TrinketUnequipCallback.EVENT.invoker().onUnequip(oldStack, ref, entity);
					TrinketsApi.getTrinket(newStack.getItem()).onEquip(newStack, ref, entity);
					TrinketEquipCallback.EVENT.invoker().onEquip(newStack, ref, entity);

					Level world = this.level();
					if (!world.isClientSide()) {
						contentUpdates.put(newRef, newStackCopy);

						if (!oldStack.isEmpty()) {
							Multimap<Holder<Attribute>, AttributeModifier> map = TrinketModifiers.get(oldStack, ref, entity);
							Multimap<String, AttributeModifier> slotMap = HashMultimap.create();
							Set<Holder<Attribute>> toRemove = Sets.newHashSet();
							for (Holder<Attribute> attr : map.keySet()) {
								if (attr.isBound() && attr.value() instanceof SlotEntityAttribute slotAttr) {
									slotMap.putAll(slotAttr.slot, map.get(attr));
									toRemove.add(attr);
								}
							}
							for (Holder<Attribute> attr : toRemove) {
								map.removeAll(attr);
							}
							//this.getAttributes().removeModifiers(map);
							map.asMap().forEach((attribute, modifiers) -> {
								AttributeInstance entityAttributeInstance = this.getAttributes().getInstance(attribute);
								if (entityAttributeInstance != null) {
									modifiers.forEach(modifier -> entityAttributeInstance.removeModifier(modifier.id()));
								}
							});

							trinkets.removeModifiers(slotMap);
						}

						if (!newStack.isEmpty()) {
							Multimap<Holder<Attribute>, AttributeModifier> map = TrinketModifiers.get(newStack, ref, entity);
							Multimap<String, AttributeModifier> slotMap = HashMultimap.create();
							Set<Holder<Attribute>> toRemove = Sets.newHashSet();
							for (Holder<Attribute> attr : map.keySet()) {
								if (attr.isBound() && attr.value() instanceof SlotEntityAttribute slotAttr) {
									slotMap.putAll(slotAttr.slot, map.get(attr));
									toRemove.add(attr);
								}
							}
							for (Holder<Attribute> attr : toRemove) {
								map.removeAll(attr);
							}
							//this.getAttributes().addTemporaryModifiers(map);
							map.forEach((attribute, attributeModifier) -> {
								AttributeInstance entityAttributeInstance = this.getAttributes().getInstance(attribute);
								if (entityAttributeInstance != null) {
									entityAttributeInstance.removeModifier(attributeModifier.id());
									entityAttributeInstance.addTransientModifier(attributeModifier);
								}

							});
							trinkets.addTemporaryModifiers(slotMap);
						}
					}
				}
				TrinketsApi.getTrinket(newStack.getItem()).tick(newStack, ref, entity);
				ItemStack tickedStack = inventory.getItem(index);
				// Avoid calling equip/unequip on stacks that mutate themselves
				if (tickedStack.getItem() == newStackCopy.getItem()) {
					newlyEquippedTrinkets.put(newRef, tickedStack.copy());
				} else {
					newlyEquippedTrinkets.put(newRef, newStackCopy);
				}
			});

			Level world = this.level();
			if (!world.isClientSide()) {
				Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

				if (!contentUpdates.isEmpty() || !inventoriesToSend.isEmpty()) {
                    Map<String, TrinketSaveData.Metadata> map = new HashMap<>();

					for (TrinketInventory trinketInventory : inventoriesToSend) {
						map.put(trinketInventory.getSlotType().getId(), trinketInventory.getSyncMetadata());
					}
                    SyncInventoryPayload packet = new SyncInventoryPayload(this.getId(), contentUpdates, map);

					for (ServerPlayer player : PlayerLookup.tracking(entity)) {
						ServerPlayNetworking.send(player, packet);
					}

					if (entity instanceof ServerPlayer serverPlayer) {
						ServerPlayNetworking.send(serverPlayer, packet);

						if (!inventoriesToSend.isEmpty()) {
							((TrinketPlayerScreenHandler) serverPlayer.inventoryMenu).trinkets$updateTrinketSlots(false);
						}
					}

					inventoriesToSend.clear();
				}
			}

			lastEquippedTrinkets.clear();
			lastEquippedTrinkets.putAll(newlyEquippedTrinkets);
		});
	}

	@Unique
	private ItemStack getOldStack(SlotType type, int index) {
		return lastEquippedTrinkets.getOrDefault(type.getGroup() + "/" + type.getName() + "/" + index, ItemStack.EMPTY);
	}
}