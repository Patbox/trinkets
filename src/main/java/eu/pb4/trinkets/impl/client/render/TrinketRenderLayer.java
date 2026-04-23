package eu.pb4.trinkets.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public class TrinketRenderLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public TrinketRenderLayer(RenderLayerParent<T, M> context, EntityRendererProvider.Context ctx) {
        super(context);
    }

    public static void extract(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketRenderState state) {
        var component = LivingEntityTrinketAttachment.get(livingEntity);
        var items = new ArrayList<TrinketRenderState.CodeRenderCall>();
        state.trinkets$setCodeRenderers(items);
        state.trinkets$setPartAttachedRenderers(new ArrayList<>());
        component.forEach((slotReference, stack) -> {
            var renderer = TrinketRendererRegistry.getRenderer(stack.getItem());
            if (renderer.isPresent()) {
                items.add(new TrinketRenderState.CodeRenderCall(slotReference, stack, renderer.get()));
            } else {
                ClientTrinketsManager.INSTANCE.get(stack).apply(livingEntity, stack, slotReference, entityState, tickDelta, state);
            }
        });
    }

    public static String replacePartName(LivingEntity livingEntity, TrinketSlotAccess access, String modelPart) {
        if (modelPart.isEmpty() || modelPart.charAt(0) != ':') {
            return modelPart;
        }

        return switch (modelPart) {
            case ":main_hand", ":mainhand", ":hand" -> livingEntity.getMainArm().getSerializedName() + "_arm";
            case ":off_hand", ":offhand" -> livingEntity.getMainArm().getOpposite().getSerializedName() + "_arm";
            default -> modelPart;
        };
    }

    public static boolean translateToModelPart(Model<?> model, String modelPart, Vector3fc offset, PoseStack poseStack) {
        var parts = ((ModelExt) model).trinkets$findPart(modelPart);

        if (parts.isEmpty()) {
            return false;
        }

        translateToModelPart(model, modelPart, parts, offset, poseStack);
        return true;
    }

    public static boolean translateToModelPartNoOffset(Model<?> model, String modelPart, PoseStack poseStack) {
        var parts = ((ModelExt) model).trinkets$findPart(modelPart);

        if (parts.isEmpty()) {
            return false;
        }

        translateToModelPartNoOffset(model, modelPart, parts, poseStack);
        return true;
    }

    public static void translateToModelPart(Model<?> model, String modelPart, List<String> parts, Vector3fc offset, PoseStack poseStack) {
        translateToModelPartNoOffset(model, modelPart, parts, poseStack);

        var bound = ((ModelExt) model).trinkets$getBounds(modelPart);

        poseStack.translate(
                bound.centerX() + bound.lX() * offset.x(),
                bound.centerY() - bound.lY() * offset.y(),
                bound.centerZ() - bound.lZ() * offset.z()
        );
    }

    public static boolean translateToModelPartNoOffset(Model<?> model, String modelPart, List<String> parts, PoseStack poseStack) {
        ModelPart part = model.root();
        part.translateAndRotate(poseStack);

        for (var p : parts) {
            part = part.getChild(p);
            part.translateAndRotate(poseStack);
        }

        if (part.hasChild("EMF_" + modelPart)) {
            part = part.getChild("EMF_" + modelPart);
            part.translateAndRotate(poseStack);
        }

        return true;
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
            var settings = o.settings();
            var parts = ((ModelExt) parent).trinkets$findPart(settings.modelPart());

            if (parts.isEmpty()) {
                continue;
            }
            poseStack.pushPose();

            translateToModelPart(parent, settings.modelPart(), parts, settings.offset(), poseStack);

            poseStack.scale(1, -1, -1);

            var bound = ((ModelExt) parent).trinkets$getBounds(settings.modelPart());
            poseStack.scale(settings.scaleTarget().scaleX(bound), settings.scaleTarget().scaleY(bound), settings.scaleTarget().scaleZ(bound));

            if (settings.transformation().isPresent()) {
                poseStack.mulPose(settings.transformation().get().getMatrix());
            }

            o.call().submit(poseStack, queue, light, OverlayTexture.NO_OVERLAY, state.outlineColor);

            poseStack.popPose();
        }
    }
}
