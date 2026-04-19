package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.ScaleTarget;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.client.render.TrinketRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Optional;

public record ItemStackTrinketElement(String modelPart, Optional<Transformation> transformation, Vector3fc offset, ScaleTarget scaleTarget,
                                      ItemDisplayContext displayContext) implements TrinketRenderElement {
    public static final MapCodec<ItemStackTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("model_part").forGetter(ItemStackTrinketElement::modelPart),
            Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(ItemStackTrinketElement::transformation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("offset", new Vector3f()).forGetter(ItemStackTrinketElement::offset),
            ScaleTarget.CODEC.optionalFieldOf("scale_target", ScaleTarget.NONE).forGetter(ItemStackTrinketElement::scaleTarget),
            ItemDisplayContext.CODEC.optionalFieldOf("display_context", ItemDisplayContext.NONE).forGetter(ItemStackTrinketElement::displayContext)
    ).apply(instance, ItemStackTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var itemStackState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(itemStackState, stack, this.displayContext, livingEntity);
        state.trinkets$getPartAttachedRenderers().add(new TrinketEntityRenderState.PartAttachedRenderer(TrinketRenderLayer.replacePartName(livingEntity, access, modelPart),
                transformation, offset, scaleTarget, itemStackState::submit));
    }
}
