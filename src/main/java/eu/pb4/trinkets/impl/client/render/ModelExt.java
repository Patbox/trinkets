package eu.pb4.trinkets.impl.client.render;

import java.util.List;

public interface ModelExt {
    List<String> trinkets$findPart(String name);
    ModelPartBounds trinkets$getBounds(String name);
}
