package eu.pb4.trinkets.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrinketRenderLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final Map<String, Bounds> storedAABBs = new HashMap<>();
    private final Map<String, List<String>> partPathCache = new HashMap<>();

    public TrinketRenderLayer(RenderLayerParent<T, M> context) {
        super(context);
    }

    public static void extract(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var component = LivingEntityTrinketAttachment.get(livingEntity);
        List<Tuple<ItemStack, TrinketSlotAccess>> items = new ArrayList<>();
        component.forEach((slotReference, stack) -> items.add(new Tuple<>(stack, slotReference)));
        state.trinkets$setItems(items);
        state.trinkets$setPartAttachedRenderers(new ArrayList<>());

        for (var x : items) {
            ClientTrinketsManager.INSTANCE.get(x.getA()).apply(livingEntity, x.getA(), x.getB(), entityState, tickDelta, state);
        }
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
    public void submit(PoseStack matrices, SubmitNodeCollector queue, int light, T state, float limbAngle, float limbDistance) {
        ((TrinketEntityRenderState) state).trinkets$getItems().forEach(pair -> {
            var renderer = TrinketRendererRegistry.getRenderer(pair.getA().getItem());
            if (renderer.isPresent()) {
                matrices.pushPose();
                renderer.get()
                        .submit(pair.getA(), pair.getB(), this.getParentModel(), matrices, queue,
                                light, state, limbAngle, limbDistance);
                matrices.popPose();
            } else {

            }
        });

        var parent = this.getParentModel();

        for (var o : ((TrinketEntityRenderState) state).trinkets$getPartAttachedRenderers()) {
            var parts = this.findPart(parent.root(), o.part());

            if (parts.isEmpty()) {
                continue;
            }

            matrices.pushPose();
            parent.root().translateAndRotate(matrices);
            ModelPart part = parent.root();
            for (var p : parts) {
                part = part.getChild(p);
                part.translateAndRotate(matrices);
            }

            var bound = this.storedAABBs.computeIfAbsent(o.part(), this::computeElementAABB);

            var offset = o.offset();

            matrices.translate(
                    bound.centerX + bound.lX * offset.x(),
                    bound.centerY - bound.lY * offset.y(),
                    bound.centerZ - bound.lZ * offset.z()
            );

            matrices.scale(1, -1, -1);

            matrices.scale(o.scaleTarget().scaleX(bound), o.scaleTarget().scaleY(bound), o.scaleTarget().scaleZ(bound));

            if (o.transformation().isPresent()) {
                matrices.mulPose(o.transformation().get().getMatrix());
            }

            o.call().submit(matrices, queue, light, OverlayTexture.NO_OVERLAY, state.outlineColor);

            matrices.popPose();
        }
    }

    private List<String> findPart(ModelPart root, String part) {
        var res = this.partPathCache.get(part);
        if (res != null) {
            return res;
        }

        for (var x : ((ModelPartAccessor) (Object) root).getChildren().entrySet()) {
            var t = findAndDefineRecursive(List.of(), part, x.getKey(), x.getValue());
            if (t != null) {
                return t;
            }
        }
        this.partPathCache.put(part, List.of());

        return List.of();
    }

    private List<String> findAndDefineRecursive(List<String> elements, String searched, String key, ModelPart value) {
        elements = new ArrayList<>(elements);
        elements.add(key);

        this.partPathCache.put(key, elements);
        if (key.equals(searched)) {
            return elements;
        }

        if (((ModelPartAccessor) (Object) value).getChildren().isEmpty()) {
            return null;
        }

        for (var x : ((ModelPartAccessor) (Object) value).getChildren().entrySet()) {
            var t = findAndDefineRecursive(elements, searched, x.getKey(), x.getValue());
            if (t != null) {
                return t;
            }
        }

        return null;
    }

    private Bounds computeElementAABB(String s) {
        var l = this.findPart(this.getParentModel().root(), s);
        if (l.isEmpty()) {
            return new Bounds(0, 0, 0, 0, 0, 0, 1, 1, 1);
        }

        var part = this.getParentModel().root();
        for (var x : l) {
            part = part.getChild(x);
        }

        if (((ModelPartAccessor) (Object) part).getCubes().isEmpty()) {
            return new Bounds(0, 0, 0, 0, 0, 0, 1, 1, 1);
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;

        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;


        for (var cube : ((ModelPartAccessor) (Object) part).getCubes()) {
            minX = Math.min(cube.minX, minX);
            minY = Math.min(cube.minY, minY);
            minZ = Math.min(cube.minZ, minZ);

            maxX = Math.max(cube.maxX, maxX);
            maxY = Math.max(cube.maxY, maxY);
            maxZ = Math.max(cube.maxZ, maxZ);
        }


        return new Bounds(
                (maxX + minX) / 2 / 16,
                (maxY + minY) / 2 / 16,
                (maxZ + minZ) / 2 / 16,
                (maxX - minX) / 2 / 16,
                (maxY - minY) / 2 / 16,
                (maxZ - minZ) / 2 / 16,
                (maxX - minX) / 16,
                (maxY - minY) / 16,
                (maxZ - minZ) / 16
        );
    }

    record Bounds(float centerX, float centerY, float centerZ, float lX, float lY, float lZ, float scaleX, float scaleY,
                  float scaleZ) {
    }
}
