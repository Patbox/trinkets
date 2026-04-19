package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.impl.client.render.types.collection.IfGroupTrinketElement;
import eu.pb4.trinkets.impl.client.render.types.collection.IfSlotTrinketElement;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

public interface TrinketRenderElements {
    ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends TrinketRenderElement>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper<>();

    Codec<TrinketRenderElement> CODEC = TrinketRenderElements.ID_MAPPER.codec(Identifier.CODEC).dispatch(TrinketRenderElement::codec, Function.identity());

    static void bootstrap() {
        ID_MAPPER.put(Identifier.withDefaultNamespace("wings"), WingsTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("equipment_replace"), EquipmentReplaceTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("item"), ItemStackTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("model"), ModelTrinketElement.CODEC);


        ID_MAPPER.put(Identifier.withDefaultNamespace("if_group"), IfGroupTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("if_slot"), IfSlotTrinketElement.CODEC);
    }
}
