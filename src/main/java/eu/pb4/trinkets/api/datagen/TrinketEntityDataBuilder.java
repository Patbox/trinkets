package eu.pb4.trinkets.api.datagen;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface TrinketEntityDataBuilder {
    TrinketEntityDataBuilder addPlayer();

    TrinketEntityDataBuilder addEntity(Identifier identifier);

    TrinketEntityDataBuilder addEntity(EntityType<?> type);

    TrinketEntityDataBuilder addEntities(TagKey<EntityType<?>> tag);

    TrinketEntityDataBuilder addSlot(String slot);

    TrinketEntityDataBuilder replace(boolean replace);
}
