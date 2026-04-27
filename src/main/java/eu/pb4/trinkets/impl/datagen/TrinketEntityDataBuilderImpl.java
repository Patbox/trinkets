package eu.pb4.trinkets.impl.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.trinkets.api.datagen.TrinketEntityDataBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.LinkedHashSet;
import java.util.Set;

public final class TrinketEntityDataBuilderImpl implements TrinketEntityDataBuilder {
    private final Set<String> entities = new LinkedHashSet<>();
    private final Set<String> slots = new LinkedHashSet<>();
    private Boolean replace = null;

    @Override
    public TrinketEntityDataBuilder addPlayer() {
        this.entities.add("minecraft:player");
        return this;
    }

    @Override
    public TrinketEntityDataBuilder addEntity(Identifier identifier) {
        this.entities.add(identifier.toString());
        return this;
    }

    @Override
    public TrinketEntityDataBuilder addEntity(EntityType<?> type) {
        this.entities.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        return this;
    }

    @Override
    public TrinketEntityDataBuilder addEntities(TagKey<EntityType<?>> tag) {
        this.entities.add("#" + tag.location());
        return this;
    }

    @Override
    public TrinketEntityDataBuilder addSlot(String slot) {
        this.slots.add(slot);
        return this;
    }

    @Override
    public TrinketEntityDataBuilder replace(boolean replace) {
        this.replace = replace;
        return this;
    }


    public JsonObject toJson() {
        var object = new JsonObject();
        if (replace != null) {
            object.addProperty("replace", this.replace);
        }
        var ent = new JsonArray();
        this.entities.forEach(ent::add);
        object.add("entities", ent);

        var s = new JsonArray();
        this.slots.forEach(s::add);
        object.add("slots", s);

        return object;
    }
}
