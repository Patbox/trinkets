package eu.pb4.trinkets.mixin.client;

import net.minecraft.client.resources.model.EquipmentAssetManager;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer.class)
public interface EquipmentLayerRendererAccessor {
    @Accessor
    EquipmentAssetManager getEquipmentAssets();
}
