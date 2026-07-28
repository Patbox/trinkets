package eu.pb4.trinkets.api.client.renderer.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.api.client.renderer.element.collection.IfGroupTrinketElement;
import eu.pb4.trinkets.api.client.renderer.element.collection.IfSlotTrinketElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class TrinketRenderElements {
    public static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends TrinketRenderElement>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper<>();

    public static final Codec<TrinketRenderElement> CODEC = ID_MAPPER.codec(Identifier.CODEC).dispatch(TrinketRenderElement::type, Function.identity());

    private TrinketRenderElements() {}

    static {
        ID_MAPPER.put(Identifier.withDefaultNamespace("wings"), WingsTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("equipment_replace"), EquipmentReplaceTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("item"), ItemStackTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("item/block"), ItemBlockStateTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("model"), ModelTrinketElement.CODEC);

        ID_MAPPER.put(Identifier.withDefaultNamespace("if_group"), IfGroupTrinketElement.CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("if_slot"), IfSlotTrinketElement.CODEC);
    }
}
