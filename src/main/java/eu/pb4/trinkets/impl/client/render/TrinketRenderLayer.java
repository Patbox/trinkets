package eu.pb4.trinkets.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TrinketRenderLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public TrinketRenderLayer(RenderLayerParent<T, M> context) {
        super(context);
    }

    public static void extract(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var component = LivingEntityTrinketAttachment.get(livingEntity);
        var items = new ArrayList<Tuple<ItemStack, TrinketSlotAccess>>();
        state.trinkets$setItems(items);
        state.trinkets$setPartAttachedRenderers(new ArrayList<>());
        component.forEach((slotReference, stack) -> {
            if (TrinketRendererRegistry.hasRenderer(stack.getItem())) {
                items.add(new Tuple<>(stack, slotReference));
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

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector queue, int light, T state, float limbAngle, float limbDistance) {
        for (var pair : ((TrinketEntityRenderState) state).trinkets$getItems()) {
            var renderer = TrinketRendererRegistry.getRenderer(pair.getA().getItem());
            if (renderer.isPresent()) {
                poseStack.pushPose();
                renderer.get()
                        .submit(pair.getA(), pair.getB(), this.getParentModel(), poseStack, queue,
                                light, state, limbAngle, limbDistance);
                poseStack.popPose();
            }
        }

        var parent = this.getParentModel();

        for (var o : ((TrinketEntityRenderState) state).trinkets$getPartAttachedRenderers()) {
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
            part = part.getChild("EMF_" +modelPart);
            part.translateAndRotate(poseStack);
        }

        return true;
    }
}
