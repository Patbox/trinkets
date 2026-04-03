package eu.pb4.trinkets.api;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

@ApiStatus.NonExtendable
public interface SlotType {
    MutableComponent getTranslation();
    String getId();
    String group();
    String name();
    int order();
    int amount();
    Identifier icon();
    boolean quickMoveCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);
    boolean validatorCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);
    boolean tooltipCheck(ItemStack stack, TrinketSlotAccess slotRef, LivingEntity entity);
    TrinketDropRule dropRule();
}
