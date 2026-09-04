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
public record IfSlotTrinketElement(List<String> slots, List<TrinketRenderElement> then, List<TrinketRenderElement> otherwise) implements TrinketRenderElement {
    public static final MapCodec<IfSlotTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.compactListCodec(Codec.STRING).fieldOf("slot").forGetter(IfSlotTrinketElement::slots),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).optionalFieldOf("then", List.of()).forGetter(IfSlotTrinketElement::then),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).optionalFieldOf("else", List.of()).forGetter(IfSlotTrinketElement::otherwise)
    ).apply(instance, IfSlotTrinketElement::new));

    public IfSlotTrinketElement(String slot, List<TrinketRenderElement> then, List<TrinketRenderElement> otherwise) {
        this(List.of(slot), then, otherwise);
    }

    @Deprecated(forRemoval = true)
    public String slot() {
        return this.slots.isEmpty() ? "" : this.slots.getFirst();
    }


    @Override
    public MapCodec<? extends TrinketRenderElement> type() {
        return CODEC;
    }

    @Override
    public Baked bake(BakingContext context) {
        var then = this.then.stream().map(r -> r.bake(context)).toList();
        var otherwise = this.otherwise.stream().map(r -> r.bake(context)).toList();

        return (owner, item, access, level, ctx, state) -> {
            if (this.slots.contains(access.slotType().getId())) {
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
