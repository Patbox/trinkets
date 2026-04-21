package eu.pb4.trinkets.mixin.client.render;

import eu.pb4.trinkets.impl.client.render.ModelExt;
import eu.pb4.trinkets.impl.client.render.ModelPartBounds;
import eu.pb4.trinkets.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(Model.class)
public abstract class ModelMixin implements ModelExt {
    @Unique
    private final Map<String, List<String>> partPathCache = new HashMap<>();
    @Unique
    private final Map<String, ModelPartBounds> modelPartBoundCache = new HashMap<>();
    @Shadow
    @Final
    protected ModelPart root;

    @Shadow
    public abstract ModelPart root();

    @Override
    public List<String> trinkets$findPart(String name) {
        var res = this.partPathCache.get(name);
        if (res != null) {
            return res;
        }

        for (var x : ((ModelPartAccessor) (Object) this.root).getChildren().entrySet()) {
            var t = findAndDefineRecursive(List.of(), name, x.getKey(), x.getValue());
            if (t != null) {
                return t;
            }
        }
        this.partPathCache.put(name, List.of());

        return List.of();
    }

    @Override
    public ModelPartBounds trinkets$getBounds(String name) {
        return this.modelPartBoundCache.computeIfAbsent(name, this::computeElementAABB);
    }

    @Unique
    private List<String> findAndDefineRecursive(List<String> elements, String searched, String key, ModelPart value) {
        elements = new ArrayList<>(elements);
        elements.add(key);

        this.partPathCache.put(key, elements);
        if (key.equals(searched)) {
            return elements;
        }

        if (((ModelPartAccessor) (Object) value).getChildren().isEmpty()) {
            return null;
        }

        for (var x : ((ModelPartAccessor) (Object) value).getChildren().entrySet()) {
            var t = findAndDefineRecursive(elements, searched, x.getKey(), x.getValue());
            if (t != null) {
                return t;
            }
        }

        return null;
    }

    @Unique
    private ModelPartBounds computeElementAABB(String s) {
        var l = this.trinkets$findPart(s);
        if (l.isEmpty()) {
            return new ModelPartBounds(0, 0, 0, 0, 0, 0, 1, 1, 1);
        }

        var part = this.root();
        for (var x : l) {
            part = part.getChild(x);
        }

        List<ModelPart.Cube> cubes = ((ModelPartAccessor) (Object) part).getCubes();

        if (cubes.isEmpty()) {
            if (part.hasChild("EMF_" + s)) {
                part = part.getChild("EMF_" + s);
                cubes = ((ModelPartAccessor) (Object) part).getCubes();

                if (cubes.isEmpty()) {
                    cubes = new ArrayList<>();
                    recursiveCubeExtraction(part, cubes::add);
                }
            }

            if (cubes.isEmpty()) {
                return new ModelPartBounds(0, 0, 0, 0, 0, 0, 1, 1, 1);
            }
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;

        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;


        for (var cube : cubes) {
            minX = Math.min(cube.minX, minX);
            minY = Math.min(cube.minY, minY);
            minZ = Math.min(cube.minZ, minZ);

            maxX = Math.max(cube.maxX, maxX);
            maxY = Math.max(cube.maxY, maxY);
            maxZ = Math.max(cube.maxZ, maxZ);
        }


        return new ModelPartBounds(
                (maxX + minX) / 2 / 16,
                (maxY + minY) / 2 / 16,
                (maxZ + minZ) / 2 / 16,
                (maxX - minX) / 2 / 16,
                (maxY - minY) / 2 / 16,
                (maxZ - minZ) / 2 / 16,
                (maxX - minX) / 16,
                (maxY - minY) / 16,
                (maxZ - minZ) / 16
        );
    }

    @Unique
    private void recursiveCubeExtraction(ModelPart part, Consumer<ModelPart.Cube> consumer) {
        ((ModelPartAccessor) (Object) part).getCubes().forEach(consumer);
        for (var x : ((ModelPartAccessor) (Object) part).getChildren().values()) {
            recursiveCubeExtraction(x, consumer);
        }
    }
}
