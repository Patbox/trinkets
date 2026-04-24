package eu.pb4.trinkets.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketsConfig;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TrinketRenderLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public TrinketRenderLayer(RenderLayerParent<T, M> context, EntityRendererProvider.Context ctx) {
        super(context);
    }

    public static void extract(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketRenderState state) {
        var component = LivingEntityTrinketAttachment.get(livingEntity);
        var items = new ArrayList<TrinketRenderState.CodeRenderCall>();
        var attached = new ArrayList<TrinketRenderState.PartAttachedRenderer>();
        state.trinkets$setCodeRenderers(items);
        state.trinkets$setPartAttachedRenderers(attached);
        component.forEach((slotReference, stack) -> {
            var renderer = TrinketRendererRegistry.getRenderer(stack.getItem());
            if (renderer.isPresent()) {
                items.add(new TrinketRenderState.CodeRenderCall(slotReference, stack, renderer.get()));
            } else {
                ClientTrinketsManager.INSTANCE.get(stack).apply(livingEntity, stack, slotReference, state, attached::add);
            }
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector queue, int light, T state, float limbAngle, float limbDistance) {
        var parent = this.getParentModel();

        for (var pair : ((TrinketRenderState) state).trinkets$getCodeRenderers()) {
            poseStack.pushPose();
            pair.renderer()
                    .submit(pair.itemStack(), pair.access(), parent, poseStack, queue, light, state, limbAngle, limbDistance);
            poseStack.popPose();
        }

        for (var o : ((TrinketRenderState) state).trinkets$getPartAttachedRenderers()) {
            submitAttached(parent, "", poseStack, queue, light, state.outlineColor, o);
        }
    }

    private static void submitAttached(Model<?> parent, String startingPart, PoseStack poseStack, SubmitNodeCollector queue, int light, int outlineColor, TrinketRenderState.PartAttachedRenderer o) {
        var settings = o.settings();
        var parts = ((ModelExt) parent).trinkets$findPart(settings.modelPart());

        if (parts.isEmpty() || (!startingPart.isEmpty() && !parts.contains(startingPart))) {
            return;
        }
        poseStack.pushPose();

        ModelAttachementImpl.translateToModelPart(parent, startingPart, settings.modelPart(), parts, settings.offset(), poseStack);

        poseStack.scale(1, -1, -1);

        var bound = ((ModelExt) parent).trinkets$getBounds(settings.modelPart());
        poseStack.scale(settings.scaleTarget().scaleX(bound), settings.scaleTarget().scaleY(bound), settings.scaleTarget().scaleZ(bound));

        if (settings.transformation().isPresent()) {
            poseStack.mulPose(settings.transformation().get().getMatrix());
        }

        o.call().submit(poseStack, queue, light, OverlayTexture.NO_OVERLAY, outlineColor);

        poseStack.popPose();
    }

    public void renderFirstPersonRightHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, LocalPlayer player) {
        if (TrinketsConfig.instance.renderFirstPersonHand && this.getParentModel() instanceof HumanoidModel<?> model) {
            var component = LivingEntityTrinketAttachment.get(player);
            var isMainHand = player.getMainArm() == HumanoidArm.RIGHT;

            component.forEach((slotReference, stack) -> {
                var renderer = TrinketRendererRegistry.getRenderer(stack.getItem());
                if (renderer.isPresent()) {
                    renderer.get().submitFirstPersonRightArm(stack, slotReference, model, model.rightArm,
                            poseStack, submitNodeCollector, light, player, isMainHand);
                } else {
                    ClientTrinketsManager.INSTANCE.get(stack).apply(player, stack, slotReference, null,
                            o -> submitAttached(model, PartNames.RIGHT_ARM, poseStack, submitNodeCollector, light, 0, o));
                }
            });
        }
    }

    public void renderFirstPersonLeftHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, LocalPlayer player) {
        if (TrinketsConfig.instance.renderFirstPersonHand && this.getParentModel() instanceof HumanoidModel<?> model) {
            var component = LivingEntityTrinketAttachment.get(player);
            var isMainHand = player.getMainArm() == HumanoidArm.LEFT;

            component.forEach((slotReference, stack) -> {
                var renderer = TrinketRendererRegistry.getRenderer(stack.getItem());
                if (renderer.isPresent()) {
                    renderer.get().submitFirstPersonLeftArm(stack, slotReference, model, model.leftArm,
                            poseStack, submitNodeCollector, light, player, isMainHand);
                } else {
                    ClientTrinketsManager.INSTANCE.get(stack).apply(player, stack, slotReference, null,
                            o -> submitAttached(model, PartNames.LEFT_ARM, poseStack, submitNodeCollector, light, 0, o));
                }
            });
        }
    }
}
