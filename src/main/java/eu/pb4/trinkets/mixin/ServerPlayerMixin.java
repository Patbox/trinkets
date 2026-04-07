package eu.pb4.trinkets.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void handleCopyingAttachment(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        LivingEntityTrinketAttachment.copyData(oldPlayer, this, restoreAll);
    }
}
