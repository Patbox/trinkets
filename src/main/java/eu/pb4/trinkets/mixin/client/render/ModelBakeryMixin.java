package eu.pb4.trinkets.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.client.render.ClientTrinketsManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelBakery.class)
public class ModelBakeryMixin {
    @Inject(method = "bakeModels", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ParallelMapTransform;schedule(Ljava/util/Map;Ljava/util/function/BiFunction;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", ordinal = 0))
    private void bakeTrinketModels(MaterialBaker materials, Executor taskExecutor, CallbackInfoReturnable<CompletableFuture<ModelBakery.BakingResult>> cir,
                                   @Local ModelBakery.ModelBakerImpl baker) {
        ClientTrinketsManager.INSTANCE.getFutureIdMap().values().forEach(x -> x.resolveModels(baker));

    }
}
