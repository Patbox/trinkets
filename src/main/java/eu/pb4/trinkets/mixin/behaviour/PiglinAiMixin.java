package eu.pb4.trinkets.mixin.behaviour;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PiglinAi.class)
public class PiglinAiMixin {
    @ModifyReturnValue(method = "isWearingSafeArmor", at = @At("TAIL"))
    private static boolean isWearingSafeTrinket(boolean original, LivingEntity livingEntity) {
        return original || TrinketsApi.getAttachment(livingEntity).isEquipped(ItemTags.PIGLIN_SAFE_ARMOR);
    }
}
