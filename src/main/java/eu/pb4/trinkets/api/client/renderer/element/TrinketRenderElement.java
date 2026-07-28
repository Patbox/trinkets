package eu.pb4.trinkets.api.client.renderer.element;

import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.renderer.TrinketRenderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface TrinketRenderElement extends ResolvableModel {
    MapCodec<? extends TrinketRenderElement> type();

    TrinketRenderElement.Baked bake(BakingContext context);

    @Override
    default void resolveDependencies(Resolver resolver) {
    }

    @Environment(EnvType.CLIENT)
    @ApiStatus.NonExtendable
    interface BakingContext extends SpecialModelRenderer.BakingContext {
        ModelBaker blockModelBaker();
    }

    @Environment(EnvType.CLIENT)
    interface Baked {
        Baked NO_OP = (_, _, _, _, _, _) -> {
        };

        void apply(LivingEntity owner, ItemStack item, TrinketSlotAccess access, @Nullable ClientLevel level, TrinketRenderContext context, @Nullable LivingEntityRenderState state);
    }
}
