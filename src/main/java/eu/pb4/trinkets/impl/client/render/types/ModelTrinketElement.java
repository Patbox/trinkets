package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.TrinketRenderState;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public record ModelTrinketElement(AttachmentSettings settings,
                                  Identifier model, List<ItemTintSource> tints,
                                  boolean centered,
                                  MutableObject<Resolved> resolvedModel) implements TrinketRenderElement {

    public static final MapCodec<ModelTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentSettings.CODEC.forGetter(ModelTrinketElement::settings),
            Identifier.CODEC.fieldOf("model").forGetter(ModelTrinketElement::model),
            ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(ModelTrinketElement::tints),
            Codec.BOOL.optionalFieldOf("centered", true).forGetter(ModelTrinketElement::centered)
    ).apply(instance, ModelTrinketElement::new));

    ModelTrinketElement(AttachmentSettings settings,
                        Identifier model, List<ItemTintSource> tints, boolean centered) {
        this(settings, model, tints, centered, new MutableObject<>());
    }

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, @Nullable TrinketRenderState state, Consumer<TrinketRenderState.PartAttachedRenderer> consumer) {
        var res = this.resolvedModel.get();
        if (res == null) {
            return;
        }

        var tint = new int[this.tints.size()];

        for (var i = 0; i < tint.length; i++) {
            tint[i] = this.tints.get(i).calculate(stack, (ClientLevel) livingEntity.level(), livingEntity);
        }

        consumer.accept(new TrinketRenderState.PartAttachedRenderer(settings.withResolvedModelPart(livingEntity, access),
                (poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor) -> {
                    if (this.centered) {
                        poseStack.translate(-0.5f, -0.5f, -0.5f);
                    }
                    submitNodeCollector.submitItem(poseStack, ItemDisplayContext.NONE, lightCoords, overlayCoords, outlineColor, tint, res.quads, ItemStackRenderState.FoilType.NONE);
                }));
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        resolver.markDependency(this.model);
    }

    @Override
    public void resolveModels(ModelBaker modelBaker) {
        var model = modelBaker.getModel(this.model);
        var baked = model.bakeTopGeometry(model.getTopTextureSlots(), modelBaker, new ModelState() {
        });

        if (baked.getAll().isEmpty()) {
            return;
        }

        this.resolvedModel.setValue(new Resolved(baked.getAll(), baked.getAll().getFirst().materialInfo().itemRenderType()));
    }

    public record Resolved(List<BakedQuad> quads, RenderType type) {
    }
}
