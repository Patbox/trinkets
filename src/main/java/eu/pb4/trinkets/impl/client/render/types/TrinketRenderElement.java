package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface TrinketRenderElement extends ResolvableModel {
    MapCodec<? extends TrinketRenderElement> codec();

    void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state);

    @Override
    default void resolveDependencies(Resolver resolver) {};

    default void resolveModels(ModelBaker modelBaker) {};
}
