package eu.pb4.trinkets.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

public class TrinketRenderLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public static final Map<Item, Function<EntityRendererProvider.Context, TrinketRenderer>> RENDERERS = new IdentityHashMap<>();

    private final ItemModelResolver itemModelResolver;
    private final BlockModelResolver blockModelResolver;
    private final Map<Item, TrinketRenderer> codeRenderers = new HashMap<>();

    public TrinketRenderLayer(RenderLayerParent<T, M> context, EntityRendererProvider.Context ctx) {
        super(context);
        this.itemModelResolver = ctx.getItemModelResolver();
        this.blockModelResolver = ctx.getBlockModelResolver();
        RENDERERS.forEach((item, renderer) -> this.codeRenderers.put(item, renderer.apply(ctx)));
    }

    public void extract(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var component = LivingEntityTrinketAttachment.get(livingEntity);
        var items = new ArrayList<TrinketEntityRenderState.CodeRenderCall>();
        var attached = new ArrayList<TrinketEntityRenderState.PartAttachedRenderer>();
        state.trinkets$setCodeRenderers(items);
        state.trinkets$setPartAttachedRenderers(attached);

        var trinketRendererState = new TrinketRenderStateFullImpl(Minecraft.getInstance(), this.itemModelResolver, this.blockModelResolver, state);

        component.forEachVisible((slotReference, stack) -> {
            var renderer = this.codeRenderers.get(stack.getItem());
            if (renderer != null) {
                items.add(new TrinketEntityRenderState.CodeRenderCall(slotReference, stack, renderer));
            } else {
                for (var baked : ClientTrinketsManager.INSTANCE.getResolved(stack)) {
                    baked.apply(livingEntity, stack, slotReference, trinketRendererState.minecraft().level, trinketRendererState, entityState);
                }
            }
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector queue, int light, T state, float limbAngle, float limbDistance) {
        var parent = this.getParentModel();

        for (var pair : ((TrinketEntityRenderState) state).trinkets$getCodeRenderers()) {
            poseStack.pushPose();
            pair.renderer()
                    .submit(pair.itemStack(), pair.access(), parent, poseStack, queue, light, state, limbAngle, limbDistance);
            poseStack.popPose();
        }

        for (var o : ((TrinketEntityRenderState) state).trinkets$getPartAttachedRenderers()) {
            submitAttached(parent, "", poseStack, queue, light, state.outlineColor, o);
        }
    }

    private static void submitAttached(Model<?> parent, String startingPart, PoseStack poseStack, SubmitNodeCollector queue, int light, int outlineColor, TrinketEntityRenderState.PartAttachedRenderer o) {
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
            var trinketRendererState = new TrinketRenderStateHandImpl(Minecraft.getInstance(), this.itemModelResolver, this.blockModelResolver,
                    o -> submitAttached(model, PartNames.RIGHT_ARM, poseStack, submitNodeCollector, light, 0, o));
            var mc = Minecraft.getInstance();

            component.forEachVisible((slotReference, stack) -> {
                var renderer = this.codeRenderers.get(stack.getItem());
                if (renderer != null) {
                    renderer.submitFirstPersonRightArm(stack, slotReference, model, model.rightArm,
                            poseStack, submitNodeCollector, light, player, isMainHand);
                } else {
                    for (var baked : ClientTrinketsManager.INSTANCE.getResolved(stack)) {
                        baked.apply(player, stack, slotReference, trinketRendererState.minecraft().level, trinketRendererState, null);
                    }
                }
            });
        }
    }

    public void renderFirstPersonLeftHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, LocalPlayer player) {
        if (TrinketsConfig.instance.renderFirstPersonHand && this.getParentModel() instanceof HumanoidModel<?> model) {
            var component = LivingEntityTrinketAttachment.get(player);
            var isMainHand = player.getMainArm() == HumanoidArm.LEFT;
            var trinketRendererState = new TrinketRenderStateHandImpl(Minecraft.getInstance(), this.itemModelResolver, this.blockModelResolver,
                    o -> submitAttached(model, PartNames.LEFT_ARM, poseStack, submitNodeCollector, light, 0, o));
            component.forEachVisible((slotReference, stack) -> {
                var renderer = this.codeRenderers.get(stack.getItem());
                if (renderer != null) {
                    renderer.submitFirstPersonLeftArm(stack, slotReference, model, model.leftArm,
                            poseStack, submitNodeCollector, light, player, isMainHand);
                } else {
                    for (var baked : ClientTrinketsManager.INSTANCE.getResolved(stack)) {
                        baked.apply(player, stack, slotReference, trinketRendererState.minecraft().level, trinketRendererState, null);
                    }
                }
            });
        }
    }
}
