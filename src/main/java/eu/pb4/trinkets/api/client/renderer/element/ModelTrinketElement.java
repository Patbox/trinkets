package eu.pb4.trinkets.api.client.renderer.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.client.renderer.AttachmentSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;

@Environment(EnvType.CLIENT)
public record ModelTrinketElement(AttachmentSettings settings,
                                  Identifier model, List<ItemTintSource> tints,
                                  boolean centered) implements TrinketRenderElement {

    public static final MapCodec<ModelTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentSettings.CODEC.forGetter(ModelTrinketElement::settings),
            Identifier.CODEC.fieldOf("model").forGetter(ModelTrinketElement::model),
            ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(ModelTrinketElement::tints),
            Codec.BOOL.optionalFieldOf("centered", true).forGetter(ModelTrinketElement::centered)
    ).apply(instance, ModelTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> type() {
        return CODEC;
    }

    @Override
    public Baked bake(BakingContext ctx) {
        var model = ctx.blockModelBaker().getModel(this.model);
        var baked = model.bakeTopGeometry(model.getTopTextureSlots(), ctx.blockModelBaker(), new ModelState() {
        });

        if (baked.getAll().isEmpty()) {
            return Baked.NO_OP;
        }

        var quads = baked.getAll();

        return (owner, item, access, level, context, state) -> {
            var tint = new int[this.tints.size()];

            for (var i = 0; i < tint.length; i++) {
                tint[i] = this.tints.get(i).calculate(item, level, owner);
            }

            context.setupAttachedRenderer(settings.withResolvedModelPart(owner, access), (poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor) -> {
                if (this.centered) {
                    poseStack.translate(-0.5f, -0.5f, -0.5f);
                }
                submitNodeCollector.submitItem(poseStack, ItemDisplayContext.NONE, lightCoords, overlayCoords, outlineColor, tint, quads, ItemStackRenderState.FoilType.NONE);
            });
        };
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        resolver.markDependency(this.model);
    }
}
