package eu.pb4.trinkets.api.client.renderer.element.collection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElements;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

@Environment(EnvType.CLIENT)
public record IfGroupTrinketElement(String group, List<TrinketRenderElement> then, List<TrinketRenderElement> otherwise) implements TrinketRenderElement {
    public static final MapCodec<IfGroupTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("group").forGetter(IfGroupTrinketElement::group),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).optionalFieldOf("then", List.of()).forGetter(IfGroupTrinketElement::then),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).optionalFieldOf("else", List.of()).forGetter(IfGroupTrinketElement::otherwise)
    ).apply(instance, IfGroupTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> type() {
        return CODEC;
    }

    @Override
    public Baked bake(BakingContext context) {
        var then = this.then.stream().map(r -> r.bake(context)).toList();
        var otherwise = this.otherwise.stream().map(r -> r.bake(context)).toList();

        return (owner, item, access, level, ctx, state) -> {
            if (this.group.equals(access.slotType().group())) {
                for (var r : then) {
                    r.apply(owner, item, access, level, ctx, state);
                }
            } else {
                for (var r : otherwise) {
                    r.apply(owner, item, access, level, ctx, state);
                }
            }
        };
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
}
