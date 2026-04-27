package eu.pb4.trinkets.api.datagen;

import com.google.common.hash.HashCode;
import com.google.gson.JsonObject;
import eu.pb4.trinkets.impl.datagen.TrinketEntityDataBuilderImpl;
import eu.pb4.trinkets.impl.datagen.TrinketSlotTypeBuilderImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public abstract class TrinketsDataProvider implements DataProvider {
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public TrinketsDataProvider(final PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = packOutput;
        this.registries = registries;
    }

    protected abstract void generate(TrinketsOutput output);

    public interface TrinketsOutput {
        TrinketEntityDataBuilder entitySlots(String name);

        /**
         * @param slotId full slot id with a group prefix
         */
        TrinketSlotTypeBuilder slotType(String slotId);
        void defineSlotGroup(String group, int order);
    }


    @Override
    public final CompletableFuture<?> run(CachedOutput cache) {
        return this.registries.thenAccept(registries -> {
            var entity = new HashMap<String, TrinketEntityDataBuilderImpl>();
            var slot = new HashMap<String, TrinketSlotTypeBuilderImpl>();
            var groups = new HashMap<String, @Nullable Integer>();

            this.generate(new TrinketsOutput() {
                @Override
                public TrinketEntityDataBuilder entitySlots(String name) {
                    return entity.computeIfAbsent(name, _ -> new TrinketEntityDataBuilderImpl());
                }

                @Override
                public TrinketSlotTypeBuilder slotType(String slotId) {
                    var split = slotId.split("/", 2);
                    if (split.length != 2) {
                        throw new IllegalStateException("Slot id needs to consist of group and slot name!");
                    }
                    groups.computeIfAbsent(split[0], _ -> null);
                    return slot.computeIfAbsent(slotId, _ -> new TrinketSlotTypeBuilderImpl());
                }

                @Override
                public void defineSlotGroup(String group, int order) {
                    groups.put(group, order);
                }
            });

            var slotPath = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("trinkets/slots/");
            for (var e : slot.entrySet()) {
                var bytes = e.getValue().toJson().toString().getBytes(StandardCharsets.UTF_8);

                try {
                    cache.writeIfNeeded(slotPath.resolve(e.getKey() + ".json"), bytes, HashCode.fromBytes(bytes));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            for (var e : groups.entrySet()) {
                var object = new JsonObject();

                if (e.getValue() != null) {
                    object.addProperty("order", e.getValue());
                }

                var bytes = object.toString().getBytes(StandardCharsets.UTF_8);

                try {
                    cache.writeIfNeeded(slotPath.resolve(e.getKey() + "/group.json"), bytes, HashCode.fromBytes(bytes));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            var entityPath = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("trinkets/entities/");
            for (var e : entity.entrySet()) {
                var bytes = e.getValue().toJson().toString().getBytes(StandardCharsets.UTF_8);

                try {
                    cache.writeIfNeeded(entityPath.resolve(e.getKey() + ".json"), bytes, HashCode.fromBytes(bytes));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
