package eu.pb4.trinkets.impl.client.render.types.collection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.client.render.types.TrinketRenderElement;
import eu.pb4.trinkets.impl.client.render.types.TrinketRenderElements;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record IfGroupTrinketElement(String group, List<TrinketRenderElement> then, List<TrinketRenderElement> otherwise) implements TrinketRenderElement {
    public static final MapCodec<IfGroupTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("group").forGetter(IfGroupTrinketElement::group),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).optionalFieldOf("then", List.of()).forGetter(IfGroupTrinketElement::then),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).optionalFieldOf("else", List.of()).forGetter(IfGroupTrinketElement::otherwise)
    ).apply(instance, IfGroupTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        if (this.group.equals(access.slotType().group())) {
            for (var r : this.then) {
                r.apply(livingEntity, stack, access, entityState, tickDelta, state);
            }
        } else {
            for (var r : this.otherwise) {
                r.apply(livingEntity, stack, access, entityState, tickDelta, state);
            }
        }
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        for (var r : then) {
            r.resolveDependencies(resolver);
        }
        for (var r : otherwise) {
            r.resolveDependencies(resolver);
        }
    }

    @Override
    public void resolveModels(ModelBaker modelBaker) {
        for (var r : then) {
            r.resolveModels(modelBaker);
        }
        for (var r : otherwise) {
            r.resolveModels(modelBaker);
        }
    }
}
