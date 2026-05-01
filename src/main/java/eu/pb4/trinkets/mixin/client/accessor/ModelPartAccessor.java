package eu.pb4.trinkets.mixin.client.accessor;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.model.geom.ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("cubes")
    List<ModelPart.Cube> trinkets$getCubes();

    @Accessor("children")
    Map<String, ModelPart> trinkets$getChildren();
}
