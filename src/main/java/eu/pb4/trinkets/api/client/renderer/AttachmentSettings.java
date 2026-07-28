package eu.pb4.trinkets.api.client.renderer;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.ModelAttachementImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Optional;


@Environment(EnvType.CLIENT)
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
                ? new AttachmentSettings(ModelAttachementImpl.replacePartName(livingEntity, access, modelPart),
                this.transformation, this.offset, this.scaleTarget) : this;
    }

    public static Builder builder(String modelPart) {
        return new Builder(modelPart);
    }

    public static final class Builder {
        private final String modelPart;
        private Optional<Transformation> transformation = Optional.empty();
        private Vector3fc offset = new Vector3f();
        private ScaleTarget scaleTarget = ScaleTarget.NONE;

        private Builder(String modelPart) {
            this.modelPart = modelPart;
        }

        public Builder transformation(@Nullable Matrix4fc matrix) {
            this.transformation = Optional.ofNullable(matrix).map(Matrix4f::new).map(Transformation::new);
            return this;
        }

        public Builder transformation(@Nullable Transformation transformation) {
            this.transformation = Optional.ofNullable(transformation);
            return this;
        }

        public Builder offset(double x, double y, double z) {
            this.offset = new Vector3f((float) x, (float) y, (float) z);
            return this;
        }

        public Builder offset(Vector3fc vector) {
            this.offset = vector;
            return this;
        }

        public Builder scale(ScaleTarget target) {
            this.scaleTarget = target;
            return this;
        }

        public AttachmentSettings build() {
            return new AttachmentSettings(this.modelPart, this.transformation, this.offset, this.scaleTarget);
        }
    }
}
