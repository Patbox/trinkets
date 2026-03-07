package eu.pb4.trinkets.impl;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import java.util.List;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

public interface TrinketEntityRenderState {

	void trinkets$setState(List<Tuple<ItemStack, TrinketSlotAccess>> items);

	List<Tuple<ItemStack, TrinketSlotAccess>> trinkets$getState();
}
