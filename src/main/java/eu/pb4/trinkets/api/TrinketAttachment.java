package eu.pb4.trinkets.api;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

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
	 * @return a specific inventory based on the provided slotId.
	 */
	@Nullable
	TrinketInventory getInventory(String slotId);

	/**
	 * @return A instance of TrinketSlotAccess. It might get invalidated in the future!
	 */
	@Nullable
	TrinketSlotAccess getSlotAccess(String slotId, int slot);

	/**
	 * @return A instance of TrinketSlotAccess. It might get invalidated in the future!
	 */
	@Nullable
	default TrinketSlotAccess getSlotAccess(TrinketSlotReference slotReference) {
		return getSlotAccess(slotReference.slot(), slotReference.index());
	}

	/**
	 * @return Whether the predicate matches any slots available to the entity
	 */
	boolean isEquipped(Predicate<ItemStack> predicate);

	/**
	 * @return Whether the item is in any slots available to the entity
	 */
	default boolean isEquipped(Item item) {
		return isEquipped(stack -> stack.is(item));
	}

	/**
	 * @return Whether the item is in any slots available to the entity
	 */
	default boolean isEquipped(TagKey<Item> item) {
		return isEquipped(stack -> stack.is(item));
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

	/**
	 * Iterates over every slot available of the entity as long as it returns true.
	 */
	void forEachWhileTrue(BiPredicate<TrinketSlotAccess, ItemStack> consumer);
}