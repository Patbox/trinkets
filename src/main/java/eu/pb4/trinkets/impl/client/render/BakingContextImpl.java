package eu.pb4.trinkets.impl.client.render;

import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.SpriteGetter;

public record BakingContextImpl(ModelBaker blockModelBaker, EntityModelSet entityModelSet,
                                SpriteGetter sprites, PlayerSkinRenderCache playerSkinRenderCache) implements TrinketRenderElement.BakingContext {
}
