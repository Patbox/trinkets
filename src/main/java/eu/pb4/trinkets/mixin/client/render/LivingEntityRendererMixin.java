package eu.pb4.trinkets.mixin.client.render;

import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.client.render.TrinketRenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the trinket feature renderer to the list of living entity features
 *
 * @author C4
 * @author powerboat9
 */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Shadow
    protected abstract boolean addLayer(RenderLayer<?, ?> layer);

    @Inject(at = @At("RETURN"), method = "<init>")
    public void init(EntityRendererProvider.Context ctx, EntityModel<?> model, float shadowRadius, CallbackInfo info) {
        //noinspection rawtypes
        this.addLayer(new TrinketRenderLayer<>((LivingEntityRenderer) (Object) this));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void updateTrinketsRenderState(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f, CallbackInfo ci) {
        var state = (TrinketEntityRenderState) livingEntityRenderState;
        TrinketRenderLayer.extract(livingEntity, livingEntityRenderState, f, state);
    }
}
