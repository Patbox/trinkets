package eu.pb4.trinkets.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TrinketFeatureRenderer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public TrinketFeatureRenderer(RenderLayerParent<T, M> context) {
        super(context);
    }

    public static void update(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var component = LivingEntityTrinketAttachment.get(livingEntity);
        List<Tuple<ItemStack, TrinketSlotAccess>> items = new ArrayList<>();
        component.forEach((slotReference, stack) -> items.add(new Tuple<>(stack, slotReference)));
        state.trinkets$setState(items);
    }

    @Override
    public void submit(PoseStack matrices, SubmitNodeCollector queue, int light, T state, float limbAngle, float limbDistance) {
        ((TrinketEntityRenderState) state).trinkets$getState().forEach(pair -> {
            var renderer = TrinketRendererRegistry.getRenderer(pair.getA().getItem());
            if (renderer.isPresent()) {
                matrices.pushPose();
                renderer.get()
                        .extractStates(pair.getA(), pair.getB(), this.getParentModel(), matrices, queue,
                                light, state, limbAngle, limbDistance);
                matrices.popPose();
            } else {

            }

        });
    }
}
