package eu.pb4.trinkets.mixin.behaviour;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }
    /*
    @ModifyReturnValue(method = "canGlide", at = @At(value = "RETURN", ordinal = 2))
    private boolean handleGliderForTrinkets(boolean original) {
        if (!original) {
            TrinketsApi.getAttachment((LivingEntity) (Object) this).isEquipped(stack -> stack.has(DataComponents.GLIDER) && !stack.nextDamageWillBreak());
        }
        return true;
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
                if (stack.has(DataComponents.GLIDER) && !stack.nextDamageWillBreak()) {
                    list.add(slot);
                }
            });

            var slotToDamage = Util.getRandom(list, this.getRandom());
            slotToDamage.get().hurtAndBreak(1, (ServerLevel) this.level(), livingEntity instanceof ServerPlayer player ? player : null, (_) -> {
                TrinketsApi.onTrinketBroken(slotToDamage.get(), slotToDamage, livingEntity);
            });
        }

        return true;
    }*/
}
