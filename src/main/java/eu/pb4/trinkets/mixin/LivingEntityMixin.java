package eu.pb4.trinkets.mixin;

import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.event.TrinketDropCallback;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketInventoryImpl;
import eu.pb4.trinkets.impl.TrinketPlayerScreenHandler;
import eu.pb4.trinkets.impl.TrinketUtilities;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import eu.pb4.trinkets.impl.platform.CommonAbstraction;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerChunkCache;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
public abstract class LivingEntityMixin extends Entity implements LivingEntityTrinketAttachment.Provider {
    @Unique
    private final Map<String, ItemStack> lastEquippedTrinkets = new HashMap<>();

    @Unique
    private final LivingEntityTrinketAttachment trinketAttachment = new LivingEntityTrinketAttachment((LivingEntity) (Object) this);

    private LivingEntityMixin() {
        super(null, null);
    }

    @Shadow
    public abstract AttributeMap getAttributes();

    @Shadow
    public abstract boolean equipmentHasChanged(ItemStack previous, ItemStack current);

    @Override
    public LivingEntityTrinketAttachment trinkets$getAttachment() {
        return this.trinketAttachment;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readTrinketData(ValueInput input, CallbackInfo ci) {
        var trinkets = input.child("trinkets");

        if (trinkets.isPresent()) {
            this.trinketAttachment.readData(trinkets.get());
        } else if (input.child("cardinal_components").isPresent()) { // Old data location
            var cardinalComponents = input.childOrEmpty("cardinal_components").child("trinkets:trinkets");
            if (cardinalComponents.isPresent()) {
                this.trinketAttachment.readData(cardinalComponents.get());
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeTrinketData(ValueOutput output, CallbackInfo ci) {
        this.trinketAttachment.writeData(output.child("trinkets"));
    }

    @Inject(at = @At("HEAD"), method = "canFreeze", cancellable = true)
    private void canFreeze(CallbackInfoReturnable<Boolean> cir) {
        for (Tuple<TrinketSlotAccess, ItemStack> equipped : this.trinketAttachment.getAllEquipped()) {
            if (equipped.getB().is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                cir.setReturnValue(false);
                break;
            }
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityEquipment;tick(Lnet/minecraft/world/entity/Entity;)V"))
    private void tickTrinkets(CallbackInfo ci) {
        this.trinketAttachment.tick();
    }

    @Inject(at = @At("TAIL"), method = "dropEquipment")
    private void dropInventory(ServerLevel world, CallbackInfo info) {
        LivingEntity entity = (LivingEntity) (Object) this;

        boolean keepInv = world.getGameRules().get(GameRules.KEEP_INVENTORY);
        this.trinketAttachment.forEach((ref, stack) -> {
            if (stack.isEmpty()) {
                return;
            }

            TrinketDropRule dropRule = TrinketsApi.getDropRule(stack, ref, entity, keepInv);

            var inventory = ref.inventory();
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
        });
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
    private void stopTrinketLocationBasedEffects(LivingEntityTrinketAttachment trinkets, final ItemStack previous, final TrinketSlotAccess inSlot, final AttributeMap attributes) {
        LivingEntity entity = (LivingEntity) (Object) this;

        TrinketUtilities.forEachModifier(entity, previous, inSlot, (attribute, modifier) -> {
            if (attribute.value() instanceof SlotAttributes.SlotModifyingAttribute x) {
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
        var trinkets = this.trinketAttachment;

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

                TrinketUtilities.callTrinketEquipmentChange(previous, newStack, ref, entity);
            }
        });

        for (var slot : changedItems) {
            var current = slot.get();
            this.lastEquippedTrinkets.put(slot.getSerializedName(), current.copy());
            if (!current.isEmpty() && !current.isBroken()) {
                TrinketUtilities.forEachModifier(entity, current, slot, (attribute, modifier) -> {
                    if (attribute.value() instanceof SlotAttributes.SlotModifyingAttribute x) {
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

        for (var a : trinkets.getInventoryImpl().values()) {
            for (var b : a.values()) {
                b.update();
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
                map.put(trinketInventory.slotType().getId(), trinketInventory.getSize());
            }
            var packet = new ClientboundCustomPayloadPacket(new SyncInventoryPayload(this.getId(), items, map));

            if (this.level().getChunkSource() instanceof ServerChunkCache cache) {
                cache.sendToTrackingPlayers(entity, packet);
            }

            if (entity instanceof ServerPlayer serverPlayer && !map.isEmpty()) {
                serverPlayer.connection.send(new ClientboundCustomPayloadPacket(new SyncInventoryPayload(this.getId(), Map.of(), map)));
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