package eu.pb4.trinkets.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.impl.client.render.LivingEntityRendererExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> implements LivingEntityRendererExt {
    public AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadow) {
        super(context, model, shadow);
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void renderHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, Identifier skinTexture, ModelPart arm, boolean hasSleeve, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        if (arm == this.model.leftArm) {
            this.trinkets$getLayer().renderFirstPersonLeftHand(poseStack, submitNodeCollector, lightCoords, Minecraft.getInstance().player);
        } else {
            this.trinkets$getLayer().renderFirstPersonRightHand(poseStack, submitNodeCollector, lightCoords, Minecraft.getInstance().player);
        }
    }
}
