package eu.pb4.trinkets.api.client.renderer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElements;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * This object represents data driven client side trinkets rendering.
 *
 * @param priority Priority of the ClientTrinket for given targets. Higher overrides lower.
 * @param target Targets for this ClientTrinket, can be empty.
 * @param render Actual list of rendered elements.
 */
@Environment(EnvType.CLIENT)
public record ClientTrinket(int priority, List<Either<Identifier, TagKey<Item>>> target, List<TrinketRenderElement> render) implements ResolvableModel {
    public static final Codec<ClientTrinket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ClientTrinket::priority),
            ExtraCodecs.compactListCodec(Codec.either(Identifier.CODEC, TagKey.hashedCodec(Registries.ITEM))).optionalFieldOf("target", List.of()).forGetter(ClientTrinket::target),
            ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).fieldOf("render").forGetter(ClientTrinket::render)
    ).apply(instance, ClientTrinket::new));

    @Override
    public void resolveDependencies(Resolver resolver) {
        for (var x : render) {
            x.resolveDependencies(resolver);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int priority = 0;
        private final List<Either<Identifier, TagKey<Item>>> target = new ArrayList<>();
        private final List<TrinketRenderElement> render = new ArrayList<>();

        private Builder() {
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder addTarget(Identifier itemId) {
            this.target.add(Either.left(itemId));

            return this;
        }

        /// Discouraged, but works. Might be removed in future version!
        /// Primarily for use with older MC releases
        public Builder addTarget(Item item) {
            this.target.add(Either.left(item.builtInRegistryHolder().key().identifier()));
            return this;
        }

        public Builder addTarget(ResourceKey<Item> item) {
            this.target.add(Either.left(item.identifier()));
            return this;
        }

        public Builder addTarget(TagKey<Item> itemTag) {
            this.target.add(Either.right(itemTag));
            return this;
        }

        public Builder addElement(TrinketRenderElement element) {
            this.render.add(element);
            return this;
        }

        public ClientTrinket build() {
            return new ClientTrinket(this.priority, this.target, this.render);
        }
    }
}
