package eu.pb4.trinkets.impl.client.render;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public interface ModelTransformConstants {
    Vector3fc FRONT = new Vector3f(0, 0, 1);
    Vector3fc BOTTOM = new Vector3f(0, -1, 0);
    Vector3fc TOP = new Vector3f(0, 1, 0);
}
