package eu.pb4.trinkets.impl.client.render;

import com.mojang.serialization.Codec;
import eu.pb4.trinkets.api.client.renderer.ClientTrinket;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientTrinketsManager extends SimpleJsonResourceReloadListener<ClientTrinketsManager.Hold> {
    public static final ClientTrinketsManager INSTANCE = new ClientTrinketsManager(ClientTrinket.CODEC.xmap(Hold::new, x -> x.clientTrinket), FileToIdConverter.json("trinkets"));
    public CompletableFuture<Map<Identifier, Hold>> completableFuture = new CompletableFuture<>();
    private Map<Identifier, Hold> idMap = Map.of();
    private Map<Item, Hold> defaultMap = Map.of();

    private Map<Identifier, Hold> futureIdMap = Map.of();

    protected ClientTrinketsManager(Codec<Hold> codec, FileToIdConverter lister) {
        super(codec, lister);
    }

    protected Map<Identifier, Hold> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var futureValues = super.prepare(manager, profiler);
        this.futureIdMap = futureValues;
        completableFuture.complete(this.futureIdMap);
        return futureValues;
    }

    @Override
    protected void apply(Map<Identifier, Hold> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.idMap = preparations;
        this.updateItemMap();
    }

    public void updateItemMap() {
        var map = new IdentityHashMap<Item, Hold>();
        for (var trinket : this.idMap.values()) {
            for (var key : trinket.clientTrinket.target()) {
                if (key.left().isPresent()) {
                    var item = BuiltInRegistries.ITEM.get(key.left().orElseThrow());
                    if (item.isPresent()) {
                        var old = map.get(item.get().value());

                        if (old == null || old.clientTrinket.priority() <= trinket.clientTrinket.priority()) {
                            map.put(item.get().value(), trinket);
                        }
                    }
                } else {
                    var tag = BuiltInRegistries.ITEM.get(key.right().orElseThrow());
                    if (tag.isPresent()) {
                        for (var item : tag.get()) {
                            var old = map.get(item.value());

                            if (old == null || old.clientTrinket.priority() <= trinket.clientTrinket.priority()) {
                                map.put(item.value(), trinket);
                            }
                        }
                    }
                }
            }
        }

        this.defaultMap = map;
    }

    public void clearItemMap() {
        this.defaultMap = Map.of();
    }

    public List<TrinketRenderElement.Baked> getResolved(ItemStack stack) {
        var trinket = stack.get(TrinketDataComponents.EQUIPMENT);
        if (trinket != null) {
            var id = trinket.assetId().map(this.idMap::get);
            if (id.isPresent()) {
                return id.get().baked;
            }
        }

        var x = this.defaultMap.get(stack.getItem());

        return x != null ? x.baked : List.of();
    }

    public void bake(BakingContextImpl baker) {
        for (var hold : this.futureIdMap.values()) {
            hold.baked = hold.clientTrinket.render().stream().map(x -> x.bake(baker)).toList();
        }
    }

    public void resolveModels(Consumer<ResolvableModel> model) {
        this.futureIdMap.values().forEach(x -> x.clientTrinket.render().forEach(model));
    }

    public static final class Hold {
        final ClientTrinket clientTrinket;
        List<TrinketRenderElement.Baked> baked = List.of();

        public Hold(ClientTrinket clientTrinket) {
            this.clientTrinket = clientTrinket;
        }
    }
}