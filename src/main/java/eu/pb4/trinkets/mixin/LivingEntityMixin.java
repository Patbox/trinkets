package eu.pb4.trinkets.mixin;

import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.event.TrinketDropCallback;
import eu.pb4.trinkets.api.event.TrinketEquipmentChangedCallback;
import eu.pb4.trinkets.impl.LivingEntityTrinketComponent;
import eu.pb4.trinkets.impl.TrinketInventoryImpl;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.impl.TrinketUtilities;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

/**
 * Trinket dropping on death, trinket EAMs, and trinket equip/unequip calls
 *
 * @author Emi
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Unique
    private final Map<String, ItemStack> lastEquippedTrinkets = new HashMap<>();

    private LivingEntityMixin() {
        super(null, null);
    }

    @Shadow
    public abstract AttributeMap getAttributes();

    @Shadow
    public abstract boolean equipmentHasChanged(ItemStack previous, ItemStack current);

    @Inject(at = @At("HEAD"), method = "canFreeze", cancellable = true)
    private void canFreeze(CallbackInfoReturnable<Boolean> cir) {
        Optional<TrinketAttachment> component = TrinketsApi.getTrinketAttachment((LivingEntity) (Object) this);
        if (component.isPresent()) {
            for (Tuple<TrinketSlotAccess, ItemStack> equipped : component.get().getAllEquipped()) {
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
        TrinketsApi.getTrinketAttachment(entity).ifPresent(trinkets -> trinkets.forEach((ref, stack) -> {
            if (stack.isEmpty()) {
                return;
            }

            TrinketDropRule dropRule = TrinketCallback.getCallback(stack).getDropRule(stack, ref, entity);

            dropRule = TrinketDropCallback.EVENT.invoker().drop(dropRule, stack, ref, entity);

            var inventory = ref.inventory();

            if (dropRule == TrinketDropRule.DEFAULT) {
                dropRule = inventory.slotType().dropRule();
            }

            if (dropRule == TrinketDropRule.DEFAULT) {
                if (keepInv && entity.getType() == EntityType.PLAYER) {
                    dropRule = TrinketDropRule.KEEP;
                } else {
                    if (EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                        dropRule = TrinketDropRule.DESTROY;
                    } else {
                        dropRule = TrinketDropRule.DROP;
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

    @Unique
    private void stopTrinketLocationBasedEffects(LivingEntityTrinketComponent trinkets, final ItemStack previous, final TrinketSlotAccess inSlot, final AttributeMap attributes) {
        LivingEntity entity = (LivingEntity) (Object) this;

        TrinketUtilities.forEachModifier(entity, previous, inSlot, (attribute, modifier) -> {
            if (attribute.value() instanceof SlotAttributes.SlotEntityAttribute x) {
                trinkets.removeModifiers(x.slot, List.of(modifier));
                return;
            }

            AttributeInstance instance = attributes.getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier);
            }
        });

        TrinketUtilities.runIterationOnItem(previous, inSlot, entity, (enchantment, level, item) -> enchantment.value().stopLocationBasedEffects(level, item, entity));
    }

    @Inject(method = "detectEquipmentUpdates", at = @At("TAIL"))
    private void handleEquipmentUpdates(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        AttributeMap attributes = this.getAttributes();
        var optional = TrinketsApi.getTrinketAttachment(entity);
        if (optional.isEmpty()) return;
        var trinkets = (LivingEntityTrinketComponent) optional.get();

        List<TrinketSlotAccess> changedItems = new ArrayList<>();
        //noinspection unchecked

        trinkets.forEach((ref, stack) -> {
            ItemStack previous = getOldStack(ref);
            ItemStack newStack = ref.get();

            if (this.equipmentHasChanged(previous, newStack)) {
                if (!previous.isEmpty()) {
                    this.stopTrinketLocationBasedEffects(trinkets, previous, ref, attributes);
                }
                changedItems.add(ref);

                TrinketEquipmentChangedCallback.EVENT.invoker().onEquipmentChanged(previous, newStack, ref, entity);
            }
        });

        for (var slot : changedItems) {
            var current = slot.get();
            this.lastEquippedTrinkets.put(slot.getSerializedName(), current.copy());
            if (!current.isEmpty() && !current.isBroken()) {
                TrinketUtilities.forEachModifier(entity, current, slot, (attribute, modifier) -> {
                    if (attribute.value() instanceof SlotAttributes.SlotEntityAttribute x) {
                        trinkets.addModifiers(x.slot, List.of(modifier));
                        return;
                    }

                    AttributeInstance instance = attributes.getInstance(attribute);
                    if (instance != null) {
                        instance.removeModifier(modifier.id());
                        instance.addTransientModifier(modifier);

                    }

                });

                if (this.level() instanceof ServerLevel serverLevel) {
                    TrinketUtilities.runIterationOnItem(current, slot, entity, (enchantment, level, item) -> enchantment.value().runLocationChangedEffects(serverLevel, level, item, entity));
                }
            }
        }
        Set<TrinketInventoryImpl> inventoriesToSend = trinkets.getContainerSizeChanged();

        if (!changedItems.isEmpty() || !inventoriesToSend.isEmpty()) {
            Map<String, Integer> map = new HashMap<>();
            Map<String, ItemStack> items = new HashMap<>();

            for (var slot : changedItems) {
                items.put(slot.getSerializedName(), slot.get().copy());
            }

            for (TrinketInventoryImpl trinketInventory : inventoriesToSend) {
                map.put(trinketInventory.slotType().getId(), trinketInventory.getContainerSize());
            }
            SyncInventoryPayload packet = new SyncInventoryPayload(this.getId(), items, map);

            for (ServerPlayer player : PlayerLookup.tracking(entity)) {
                ServerPlayNetworking.send(player, packet);
            }

            if (entity instanceof ServerPlayer serverPlayer && !map.isEmpty()) {
                ServerPlayNetworking.send(serverPlayer, new SyncInventoryPayload(this.getId(), Map.of(), map));
                ((TrinketPlayerScreenHandler) serverPlayer.inventoryMenu).trinkets$updateTrinketSlots(false);
            }

            inventoriesToSend.clear();
        }
    }

    @Unique
    private ItemStack getOldStack(TrinketSlotAccess access) {
        return lastEquippedTrinkets.getOrDefault(access.getSerializedName(), ItemStack.EMPTY);
    }
}