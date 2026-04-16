package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;

public interface TrinketEntityRenderState {
	void trinkets$setItems(List<Tuple<ItemStack, TrinketSlotAccess>> items);
	List<Tuple<ItemStack, TrinketSlotAccess>> trinkets$getItems();
	void trinkets$setEquipmentOverride(EquipmentSlot slot, EquipmentOverride override);
	@Nullable
	EquipmentOverride trinkets$getEquipmentOverride(EquipmentSlot slot);
	void trinkets$setWingOverride(EquipmentOverride override);
	@Nullable
	EquipmentOverride trinkets$getWingOverride();


	record EquipmentOverride(TrinketSlotAccess access, ItemStack stack,
							 boolean force,
							 Optional<ResourceKey<EquipmentAsset>> assetResourceKey) {

	};
}
