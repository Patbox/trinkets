package eu.pb4.trinkets.api.client.datagen;

import com.mojang.datafixers.util.Either;
import eu.pb4.trinkets.api.client.renderer.ClientTrinket;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public abstract class ClientTrinketsProvider implements DataProvider {
    private final PackOutput output;

    public ClientTrinketsProvider(final PackOutput packOutput) {
        this.output = packOutput;
    }

    protected abstract void generate(ClientTrinketsOutput output);

    public interface ClientTrinketsOutput {
        void acceptClientTrinket(Identifier identifier, ClientTrinket trinket);

        default void acceptClientTrinket(ResourceKey<Item> item, List<TrinketRenderElement> elements) {
            acceptClientTrinket(item.identifier(), new ClientTrinket(0, List.of(Either.left(item.identifier())), elements));
        }

        default void acceptClientTrinket(TagKey<Item> itemTagKey, List<TrinketRenderElement> elements) {
            acceptClientTrinket(itemTagKey.location().withSuffix("_tagged"), new ClientTrinket(-500, List.of(Either.right(itemTagKey)), elements));
        }

        default void acceptClientTrinket(TagKey<Item> itemTagKey, int priority, List<TrinketRenderElement> elements) {
            acceptClientTrinket(itemTagKey.location().withSuffix("_tagged"), new ClientTrinket(priority, List.of(Either.right(itemTagKey)), elements));
        }
    }


    @Override
    public final CompletableFuture<?> run(CachedOutput cache) {
        var clientTrinkets = new HashMap<Identifier, ClientTrinket>();

        this.generate(new ClientTrinketsOutput() {
            @Override
            public void acceptClientTrinket(Identifier identifier, ClientTrinket trinket) {
                clientTrinkets.put(identifier, trinket);
            }
        });

        return DataProvider.saveAll(cache, ClientTrinket.CODEC, this.output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "trinkets"), clientTrinkets);
    }
}
