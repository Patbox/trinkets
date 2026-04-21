package eu.pb4.trinkets.impl.client.render;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ScaleTarget implements StringRepresentable {
    NONE {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }
    },
    X {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return bound.scaleX();
        }
    },
    Y {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    Z {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },
    XZ_MIN {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleX(), bound.scaleZ());
        }
    },
    XZ_MAX {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleX(), bound.scaleZ());
        }
    },
    XY_MIN {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleX(), bound.scaleY());
        }
    },
    XY_MAX {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleX(), bound.scaleY());
        }
    },
    YZ_MIN {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleY(), bound.scaleZ());
        }
    },
    YZ_MAX {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleY(), bound.scaleZ());
        }
    },
    XYZ_MIN {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.min(bound.scaleY(), Math.min(bound.scaleX(), bound.scaleZ()));
        }
    },
    XYZ_MAX {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return Math.max(bound.scaleY(), Math.min(bound.scaleX(), bound.scaleZ()));
        }
    },

    XYZ_STRETCH {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }

        @Override
        float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }

        @Override
        float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },

    XZ_STRETCH {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }

        @Override
        float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },
    XY_STRETCH {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }

        @Override
        float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    YZ_STRETCH {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }

        @Override
        float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    X_STRETCH {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        float scaleX(ModelPartBounds bound) {
            return bound.scaleX();
        }
    },
    Y_STRETCH {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        float scaleY(ModelPartBounds bound) {
            return bound.scaleY();
        }
    },
    Z_STRETCH {
        @Override
        float scaleVal(ModelPartBounds bound) {
            return 1;
        }

        @Override
        float scaleZ(ModelPartBounds bound) {
            return bound.scaleZ();
        }
    },

    ;

    public static final Codec<ScaleTarget> CODEC = StringRepresentable.fromEnum(ScaleTarget::values);

    abstract float scaleVal(ModelPartBounds bound);

    float scaleX(ModelPartBounds bound) {
        return scaleVal(bound);
    }

    float scaleY(ModelPartBounds bound) {
        return scaleVal(bound);
    }

    float scaleZ(ModelPartBounds bound) {
        return scaleVal(bound);
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
