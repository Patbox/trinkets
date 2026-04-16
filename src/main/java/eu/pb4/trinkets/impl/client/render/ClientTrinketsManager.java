package eu.pb4.trinkets.impl.client.render;

import com.mojang.serialization.Codec;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

public class ClientTrinketsManager extends SimpleJsonResourceReloadListener<ClientTrinket> {
    public static final ClientTrinketsManager INSTANCE = new ClientTrinketsManager(ClientTrinket.CODEC, FileToIdConverter.json("trinkets"));
    private Map<Identifier, ClientTrinket> idMap = Map.of();
    private Map<Item, ClientTrinket> defaultMap = Map.of();

    protected ClientTrinketsManager(Codec<ClientTrinket> codec, FileToIdConverter lister) {
        super(codec, lister);
    }

    @Override
    protected void apply(Map<Identifier, ClientTrinket> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.idMap = preparations;
    }

    public void updateItemMap() {
        var map = new IdentityHashMap<Item, ClientTrinket>();
        for (var trinket : this.idMap.values()) {
            for (var key : trinket.target()) {
                if (key.left().isPresent()) {
                    var item = BuiltInRegistries.ITEM.get(key.left().orElseThrow());
                    if (item.isPresent()) {
                        map.put(item.get().value(), trinket);
                    }
                } else {
                    var tag = BuiltInRegistries.ITEM.get(key.right().orElseThrow());
                    if (tag.isPresent()) {
                        for (var item : tag.get()) {
                            map.put(item.value(), trinket);
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

    public ClientTrinket get(ItemStack stack) {
        var trinket = stack.get(TrinketDataComponents.EQUIPMENT);
        if (trinket != null) {
            // Todo.
        }

        return this.defaultMap.getOrDefault(stack.getItem(), ClientTrinket.EMPTY);
    }
}