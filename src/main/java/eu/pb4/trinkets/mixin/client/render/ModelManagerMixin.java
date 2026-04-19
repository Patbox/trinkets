package eu.pb4.trinkets.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.trinkets.impl.client.render.ClientTrinketsManager;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(ModelManager.class)
public class ModelManagerMixin {
    @Inject(method = "discoverModelDependencies*", at = @At(value = "INVOKE", target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V", ordinal = 0), require = 0)
    private static void resolveTrinketModels(CallbackInfoReturnable<Object> cir, @Local(name = "result") ModelDiscovery result) {
        ClientTrinketsManager.INSTANCE.completableFuture = new CompletableFuture<>();
        ClientTrinketsManager.INSTANCE.getFutureIdMap().values().forEach(result::addRoot);
    }

    @ModifyExpressionValue(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;", ordinal = 0))
    private static CompletableFuture<Void> waitForTrinketsAssets(CompletableFuture<Void> original) {
        return CompletableFuture.allOf(original, ClientTrinketsManager.INSTANCE.completableFuture);
    }
}
