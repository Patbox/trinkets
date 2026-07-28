package eu.pb4.trinkets.api.client.renderer;

import com.mojang.serialization.Codec;
import eu.pb4.trinkets.impl.client.render.ModelPartBounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public enum ScaleTarget implements StringRepresentable {
    NONE {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }
    },
    X {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return bound.scaleX();
        }
    },
    Y {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    Z {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },
    XZ_MIN {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleX(), bound.scaleZ());
        }
    },
    XZ_MAX {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleX(), bound.scaleZ());
        }
    },
    XY_MIN {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleX(), bound.scaleY());
        }
    },
    XY_MAX {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleX(), bound.scaleY());
        }
    },
    YZ_MIN {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleY(), bound.scaleZ());
        }
    },
    YZ_MAX {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleY(), bound.scaleZ());
        }
    },
    XYZ_MIN {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleY(), Math.min(bound.scaleX(), bound.scaleZ()));
        }
    },
    XYZ_MAX {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleY(), Math.min(bound.scaleX(), bound.scaleZ()));
        }
    },

    XYZ_STRETCH {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        public float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }

        @Override
        public float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }

        @Override
        public float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },

    XZ_STRETCH {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        public float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }

        @Override
        public float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },
    XY_STRETCH {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        public float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }

        @Override
        public float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    YZ_STRETCH {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        public float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }

        @Override
        public float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    X_STRETCH {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        public float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }
    },
    Y_STRETCH {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        public float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    Z_STRETCH {
        @Override
        public float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        public float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },

    ;

    public static final Codec<ScaleTarget> CODEC = StringRepresentable.fromEnum(ScaleTarget::values);

    public abstract float scaleVal(ModelPartBounds bound);

    public float scaleX(ModelPartBounds bound) {
        return scaleVal(bound);
    }

    public float scaleY(ModelPartBounds bound) {
        return scaleVal(bound);
    }

    public float scaleZ(ModelPartBounds bound) {
        return scaleVal(bound);
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
