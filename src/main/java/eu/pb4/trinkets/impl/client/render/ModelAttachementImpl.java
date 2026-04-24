package eu.pb4.trinkets.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3fc;

import java.util.List;

public class ModelAttachementImpl {
    public static boolean skipCache = false;

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

    public static boolean translateToModelPart(Model<?> model, String source, String modelPart, Vector3fc offset, PoseStack poseStack) {
        var parts = ((ModelExt) model).trinkets$findPart(modelPart);

        if (parts.isEmpty()) {
            return false;
        }

        return translateToModelPart(model, source, modelPart, parts, offset, poseStack);
    }

    public static boolean translateToModelPartNoOffset(Model<?> model, String source, String modelPart, PoseStack poseStack) {
        var parts = ((ModelExt) model).trinkets$findPart(modelPart);

        if (parts.isEmpty()) {
            return false;
        }

        return translateToModelPartNoOffset(model, source, modelPart, parts, poseStack);
    }

    public static boolean translateToModelPart(Model<?> model, String source, String modelPart, List<String> parts, Vector3fc offset, PoseStack poseStack) {
        if (translateToModelPartNoOffset(model, source, modelPart, parts, poseStack)) {
            var bound = ((ModelExt) model).trinkets$getBounds(modelPart);

            poseStack.translate(
                    bound.centerX() + bound.lX() * offset.x(),
                    bound.centerY() - bound.lY() * offset.y(),
                    bound.centerZ() - bound.lZ() * offset.z()
            );

            return true;
        }

        return false;
    }

    public static boolean translateToModelPartNoOffset(Model<?> model, String source, String modelPart, List<String> parts, PoseStack poseStack) {
        var canTransform = source.isEmpty();

        ModelPart part = model.root();
        if (canTransform) {
            part.translateAndRotate(poseStack);
        }

        for (var p : parts) {
            part = part.getChild(p);
            if (canTransform || p.equals(source)) {
                if (canTransform || !part.hasChild("EMF_" + modelPart)) { // Bit of a hack to workaround issues with EMF
                    part.translateAndRotate(poseStack);
                }
                canTransform = true;
            }
        }

        if (canTransform && part.hasChild("EMF_" + modelPart)) {
            part = part.getChild("EMF_" + modelPart);
            part.translateAndRotate(poseStack);
        }

        return canTransform;
    }
}
