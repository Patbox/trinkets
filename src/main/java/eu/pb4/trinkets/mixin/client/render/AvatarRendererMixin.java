package eu.pb4.trinkets.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.impl.client.render.LivingEntityRendererExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin implements LivingEntityRendererExt {
    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void renderRightHandTrinkets(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                        int lightCoords, Identifier skinTexture, boolean hasSleeve, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            this.trinkets$getLayer().renderFirstPersonRightHand(poseStack, submitNodeCollector, lightCoords, Minecraft.getInstance().player);
        }
    }


    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void renderLeftHandTrinkets(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                        int lightCoords, Identifier skinTexture, boolean hasSleeve, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            this.trinkets$getLayer().renderFirstPersonLeftHand(poseStack, submitNodeCollector, lightCoords, Minecraft.getInstance().player);
        }
    }
}
