package eu.pb4.trinkets.mixin.client.behaviour;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "findTotem", at = @At("TAIL"), cancellable = true)
    private static void findTrinketTotem(Player player, CallbackInfoReturnable<ItemStack> cir) {
        LivingEntityTrinketAttachment.get(player).findFirst(x -> x.has(DataComponents.DEATH_PROTECTION))
                .map(TrinketSlotAccess::get).ifPresent(cir::setReturnValue);
    }
}
