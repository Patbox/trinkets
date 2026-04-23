package eu.pb4.trinkets.impl.client.render;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.types.TrinketRenderElement;
import eu.pb4.trinkets.impl.client.render.types.TrinketRenderElements;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ClientTrinket(List<Either<Identifier, TagKey<Item>>> target, List<TrinketRenderElement> render) implements ResolvableModel {
    public static final Codec<ClientTrinket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.compactListCodec(Codec.either(Identifier.CODEC, TagKey.hashedCodec(Registries.ITEM))).optionalFieldOf("target", List.of()).forGetter(ClientTrinket::target),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).fieldOf("render").forGetter(ClientTrinket::render)
    ).apply(instance, ClientTrinket::new));

    public static final ClientTrinket EMPTY = new ClientTrinket(List.of(), List.of());

    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketRenderState state) {
        for (var x : render) {
            x.apply(livingEntity, stack, access, entityState, tickDelta, state);
        }
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        for (var x : render) {
            x.resolveDependencies(resolver);
        }
    }

    public void resolveModels(ModelBaker bakery) {
        for (var x : render) {
            x.resolveModels(bakery);
        }
    }
}
