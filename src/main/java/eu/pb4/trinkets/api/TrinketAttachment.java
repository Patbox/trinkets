package eu.pb4.trinkets.api;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface TrinketAttachment {

	LivingEntity getEntity();

	/**
	 * @return A map of names to slot groups available to the entity
	 */
	Map<String, SlotGroup> getGroups();

	/**
	 * @return A map of slot group names, to slot names, to trinket inventories
	 * for the entity. Inventories will respect EAM slot count modifications for
	 * the entity.
	 */
	Map<String, Map<String, TrinketInventory>> getInventory();

	/**
	 * @return Whether the predicate matches any slots available to the entity
	 */
	boolean isEquipped(Predicate<ItemStack> predicate);

	/**
	 * @return Whether the item is in any slots available to the entity
	 */
	default boolean isEquipped(Item item) {
		return isEquipped(stack -> stack.getItem() == item);
	}

	/**
	 * @return All slots that match the provided predicate
	 */
	List<Tuple<TrinketSlotAccess, ItemStack>> getEquipped(Predicate<ItemStack> predicate);

	/**
	 * @return All slots that contain the provided item
	 */
	default List<Tuple<TrinketSlotAccess, ItemStack>> getEquipped(Item item) {
		return getEquipped(stack -> stack.is(item));
	}

	/**
	 * @return All non-empty slots
	 */
	default List<Tuple<TrinketSlotAccess, ItemStack>> getAllEquipped() {
		return getEquipped(stack -> !stack.isEmpty());
	}

	/**
	 * Iterates over every slot available to the entity
	 */
	void forEach(BiConsumer<TrinketSlotAccess, ItemStack> consumer);
}