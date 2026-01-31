package eu.pb4.trinkets.impl.payload;

import eu.pb4.trinkets.impl.TrinketsNetwork;
import eu.pb4.trinkets.api.SlotGroup;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;

public record SyncSlotsPayload(Map<EntityType<?>, Map<String, SlotGroup>> map) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncSlotsPayload> CODEC = ByteBufCodecs.map(
			(x) -> (Map<EntityType<?>, Map<String, SlotGroup>>) new HashMap<EntityType<?>, Map<String, SlotGroup>>(x),
			ByteBufCodecs.registry(Registries.ENTITY_TYPE),
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.COMPOUND_TAG.map(SlotGroup::read, (x) -> {
				CompoundTag nbt = new CompoundTag();
				x.write(nbt);
				return nbt;
			}))
			).map(SyncSlotsPayload::new, SyncSlotsPayload::map);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TrinketsNetwork.SYNC_SLOTS;
	}
}