package eu.pb4.trinkets.mixin;

import eu.pb4.trinkets.impl.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.TrinketFeatureRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
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
    @Invoker("addLayer")
    public abstract boolean invokeAddFeature(RenderLayer<?, ?> feature);

	@Inject(at = @At("RETURN"), method = "<init>")
	public void init(EntityRendererProvider.Context ctx, EntityModel<?> model, float shadowRadius, CallbackInfo info) {
        this.invokeAddFeature(new TrinketFeatureRenderer<>((RenderLayerParent) (Object) this));
	}

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void updateTrinketsRenderState(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f, CallbackInfo ci) {
        var state = (TrinketEntityRenderState) livingEntityRenderState;
        TrinketFeatureRenderer.update(livingEntity, livingEntityRenderState, f, state);
    }
}
