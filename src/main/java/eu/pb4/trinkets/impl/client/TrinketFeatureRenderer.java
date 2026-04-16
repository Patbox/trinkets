package eu.pb4.trinkets.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.client.render.ClientTrinket;
import eu.pb4.trinkets.impl.client.render.ClientTrinketsManager;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrinketFeatureRenderer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public TrinketFeatureRenderer(RenderLayerParent<T, M> context) {
        super(context);
    }

    public static void extract(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var component = LivingEntityTrinketAttachment.get(livingEntity);
        List<Tuple<ItemStack, TrinketSlotAccess>> items = new ArrayList<>();
        component.forEach((slotReference, stack) -> items.add(new Tuple<>(stack, slotReference)));
        state.trinkets$setItems(items);

        for (var x : items) {
            ClientTrinketsManager.INSTANCE.get(x.getA()).apply(livingEntity, x.getA(), x.getB(), entityState, tickDelta, state);
        }
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
    }
}
