package eu.pb4.trinkets.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void renderLeftHandTrinkets(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                        int lightCoords, Identifier skinTexture, boolean hasSleeve, CallbackInfo ci) {

    }
}
