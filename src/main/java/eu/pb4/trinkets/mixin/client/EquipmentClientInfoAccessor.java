package eu.pb4.trinkets.mixin.client;

import com.mojang.serialization.Codec;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.resources.model.EquipmentClientInfo.class)
public interface EquipmentClientInfoAccessor {
    @Accessor
    static Codec<List<EquipmentClientInfo.Layer>> getLAYER_LIST_CODEC() {
        throw new UnsupportedOperationException();
    }
}
