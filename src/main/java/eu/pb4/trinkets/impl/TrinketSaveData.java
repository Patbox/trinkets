package eu.pb4.trinkets.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;


public record TrinketSaveData(Map<String, Map<String, InventoryData>> data) {
    public static final Codec<TrinketSaveData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, InventoryData.CODEC)).xmap(TrinketSaveData::new, TrinketSaveData::data);
    public static final MapCodec<TrinketSaveData> MAP_CODEC = MapCodec.assumeMapUnsafe(CODEC);

    public record Metadata(List<AttributeModifier> persistentModifiers, List<AttributeModifier> cachedModifiers) {
        public static final Metadata EMPTY = new Metadata(List.of(), List.of());
        public static final Codec<Metadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AttributeModifier.CODEC.listOf().optionalFieldOf("PersistentModifiers", List.of()).forGetter(Metadata::persistentModifiers),
                AttributeModifier.CODEC.listOf().optionalFieldOf("CachedModifiers", List.of()).forGetter(Metadata::cachedModifiers)
        ).apply(instance, Metadata::new));
    }
    public record InventoryData(Metadata metadata, List<ItemStack> items, int inventorySize) {
        public static final Codec<InventoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Metadata.CODEC.optionalFieldOf("Metadata", Metadata.EMPTY).forGetter(InventoryData::metadata),
                ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("Items", List.of()).forGetter(InventoryData::items),
                Codec.INT.optionalFieldOf("size", -1).forGetter(InventoryData::inventorySize)
        ).apply(instance, InventoryData::new));
    }
}