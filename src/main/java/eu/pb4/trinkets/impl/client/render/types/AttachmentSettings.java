package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.ScaleTarget;
import eu.pb4.trinkets.impl.client.render.TrinketRenderLayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Optional;

public record AttachmentSettings(String modelPart, Optional<Transformation> transformation, Vector3fc offset,
                                 ScaleTarget scaleTarget) {
    public static final MapCodec<AttachmentSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("model_part").forGetter(AttachmentSettings::modelPart),
            Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(AttachmentSettings::transformation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("offset", new Vector3f()).forGetter(AttachmentSettings::offset),
            ScaleTarget.CODEC.optionalFieldOf("scale_target", ScaleTarget.NONE).forGetter(AttachmentSettings::scaleTarget)
    ).apply(instance, AttachmentSettings::new));

    public AttachmentSettings withResolvedModelPart(LivingEntity livingEntity, TrinketSlotAccess access) {
        return !this.modelPart.isEmpty() && this.modelPart.charAt(0) == ':'
                ? new AttachmentSettings(TrinketRenderLayer.replacePartName(livingEntity, access, modelPart),
                this.transformation, this.offset, this.scaleTarget) : this;
    }
}
