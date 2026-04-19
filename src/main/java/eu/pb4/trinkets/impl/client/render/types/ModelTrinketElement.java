package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.ScaleTarget;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.client.render.TrinketRenderLayer;
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
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Optional;

public record ModelTrinketElement(String modelPart, Optional<Transformation> transformation, Vector3fc offset, ScaleTarget scaleTarget,
                                  Identifier model, List<ItemTintSource> tints,

                                  boolean centered,
                                  MutableObject<Resolved> resolvedModel) implements TrinketRenderElement {

    public static final MapCodec<ModelTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("model_part").forGetter(ModelTrinketElement::modelPart),
            Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(ModelTrinketElement::transformation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("offset", new Vector3f()).forGetter(ModelTrinketElement::offset),
            ScaleTarget.CODEC.optionalFieldOf("scale_target", ScaleTarget.NONE).forGetter(ModelTrinketElement::scaleTarget),
            Identifier.CODEC.fieldOf("model").forGetter(ModelTrinketElement::model),
            ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(ModelTrinketElement::tints),
            Codec.BOOL.optionalFieldOf("centered", true).forGetter(ModelTrinketElement::centered)
    ).apply(instance, ModelTrinketElement::new));

    ModelTrinketElement(String modelPart, Optional<Transformation> transformation, Vector3fc offset, ScaleTarget scaleTarget,
                        Identifier model, List<ItemTintSource> tints, boolean centered) {
        this(modelPart, transformation, offset, scaleTarget, model, tints, centered, new MutableObject<>());
    }

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var res = this.resolvedModel.get();
        if (res == null) {
            return;
        }

        var tint = new int[this.tints.size()];

        for (var i = 0; i < tint.length; i++) {
            tint[i] = this.tints.get(i).calculate(stack, (ClientLevel) livingEntity.level(), livingEntity);
        }

        state.trinkets$getPartAttachedRenderers().add(new TrinketEntityRenderState.PartAttachedRenderer(TrinketRenderLayer.replacePartName(livingEntity, access, modelPart),
                transformation, offset, scaleTarget,
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
