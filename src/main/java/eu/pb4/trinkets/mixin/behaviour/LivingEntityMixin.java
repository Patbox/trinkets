package eu.pb4.trinkets.mixin.behaviour;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    @Final
    @Mutable
    public static Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM;

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void handleGazeHiding(CallbackInfo ci) {
        PLAYER_NOT_WEARING_DISGUISE_ITEM = PLAYER_NOT_WEARING_DISGUISE_ITEM.or(entity -> TrinketsApi.getAttachment(entity).isEquipped(ItemTags.GAZE_DISGUISE_EQUIPMENT, true));
    }

    @Inject(method = "canGlide", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"), cancellable = true)
    private void handleGliderForTrinkets(CallbackInfoReturnable<Boolean> cir) {
        if (TrinketsApi.getAttachment((LivingEntity) (Object) this).isEquipped(stack -> stack.has(DataComponents.GLIDER) && !stack.nextDamageWillBreak(), true)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
    private List<EquipmentSlot> addAFallback(List<EquipmentSlot> original, @Share("is_faux") LocalBooleanRef ref) {
        if (original.isEmpty()) {
            ref.set(true);
            return List.of(EquipmentSlot.BODY);
        }
        ref.set(false);
        return original;
    }

    @WrapWithCondition(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private boolean preventFalseBreak(ItemStack instance, int serverLevel, LivingEntity livingEntity, EquipmentSlot amount, @Share("is_faux") LocalBooleanRef ref) {
        if (ref.get()) {
            var list = new ArrayList<TrinketSlotAccess>();
            TrinketsApi.getAttachment((LivingEntity) (Object) this).forEach((slot, stack) -> {
                if (stack.has(DataComponents.GLIDER) && !stack.nextDamageWillBreak() && TrinketsApi.canApplyEffects(stack, slot, livingEntity)) {
                    list.add(slot);
                }
            });

            var slotToDamage = Util.getRandom(list, this.getRandom());
            slotToDamage.get().hurtAndBreak(1, (ServerLevel) this.level(), livingEntity instanceof ServerPlayer player ? player : null, (_) -> {
                TrinketsApi.onTrinketBroken(slotToDamage.get(), slotToDamage, livingEntity);
            });
        }

        return true;
    }

    @Expression("? != null")
    @Inject(method = "checkTotemDeathProtection", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 1, shift = At.Shift.BEFORE))
    private void findTrinketTotem(CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) LocalRef<ItemStack> itemStack, @Local LocalRef<DeathProtection> protection) {
        if (protection.get() != null) {
            return;
        }

        var totem = LivingEntityTrinketAttachment.get((LivingEntity) (Object) this).findFirst(x -> x.has(DataComponents.DEATH_PROTECTION), true);

        if (totem.isPresent()) {
            var stack = totem.get().get();
            protection.set(stack.get(DataComponents.DEATH_PROTECTION));
            var sc = stack.copy();
            itemStack.set(sc);
            stack.shrink(1);
        }
    }
}
