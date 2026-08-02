package eu.pb4.trinkets.mixin;

import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.impl.*;
import eu.pb4.trinkets.impl.client.TrinketsClient;
import eu.pb4.trinkets.impl.slots.SurvivalTrinketSlot;
import eu.pb4.trinkets.impl.slots.TrinketSlotState;
import eu.pb4.trinkets.impl.slots.TrinketSlotStateImpl;
import eu.pb4.trinkets.mixin.accessor.AbstractedContainerMenuAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds trinket slots to the player's screen handler
 *
 * @author Emi
 */
@Mixin(value = InventoryMenu.class, priority = 500)
public abstract class InventoryMenuMixin extends AbstractContainerMenu implements TrinketInventoryMenu {
    @Shadow
    @Final
    private Player owner;
    @Unique
    private int trinketSlotStart = 0;
    @Unique
    private int trinketSlotEnd = 0;

    @Unique
    private TrinketSlotState trinketsSlotState;

    private InventoryMenuMixin() {
        super(null, 0);
    }

    @Inject(at = @At("RETURN"), method = "<init>")
    private void init(Inventory playerInv, boolean onServer, Player owner, CallbackInfo info) {
        trinkets$updateTrinketSlots(true);
    }

    @Override
    public void trinkets$updateTrinketSlots(boolean reinitializeAttachment) {
        var trinkets = LivingEntityTrinketAttachment.get(owner);

        if (reinitializeAttachment) {
            trinkets.rebuild();
        }

        this.trinketsSlotState = TrinketSlotStateImpl.create(this.owner, this, trinkets);

        while (trinketSlotStart < trinketSlotEnd) {
            slots.remove(trinketSlotStart);
            ((AbstractedContainerMenuAccessor) (this)).trinkets$getLastSlots().remove(trinketSlotStart);
            ((AbstractedContainerMenuAccessor) (this)).trinkets$getRemoteSlots().remove(trinketSlotStart);
            trinketSlotEnd--;
        }
        trinketSlotStart = slots.size();
        this.trinketsSlotState.createSlots(this::addSlot);
        trinketSlotEnd = slots.size();
    }

    @Override
    public int trinkets$getTrinketSlotStart() {
        return trinketSlotStart;
    }

    @Override
    public int trinkets$getTrinketSlotEnd() {
        return trinketSlotEnd;
    }

    @Inject(at = @At("HEAD"), method = "removed")
    private void onClosed(Player player, CallbackInfo info) {
        Level world = player.level();
        if (TrinketsMain.IS_CLIENT && world.isClientSide()) {
            TrinketsClient.activeGroup = null;
            TrinketsClient.activeType = null;
            TrinketsClient.quickMoveGroup = null;
        }
    }

    @Override
    public TrinketSlotState trinkets$getSlotState() {
        return this.trinketsSlotState;
    }

    @Inject(at = @At("HEAD"), method = "quickMoveStack", cancellable = true)
    private void quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> info) {
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            if (index >= trinketSlotStart && index < trinketSlotEnd) {
                if (!this.moveItemStackTo(stack, 9, 45, false)) {
                    info.setReturnValue(ItemStack.EMPTY);
                } else {
                    info.setReturnValue(stack);
                }
            } else if (index >= 9 && index < 45) {
                for (int i = trinketSlotStart; i < trinketSlotEnd; i++) {
                    Slot s = slots.get(i);
                    if (!(s instanceof SurvivalTrinketSlot ts) || !s.mayPlace(stack)) {
                        continue;
                    }

                    SlotType type = ts.getType();
                    TrinketSlotAccess ref = new TrinketSlotAccess((TrinketInventory) ts.container, ts.getContainerSlot());

                    boolean res = type.quickMoveCheck(stack, ref, player);

                    if (res) {
                        if (this.moveItemStackTo(stack, i, i + 1, false)) {
                            Level world = player.level();
                            if (TrinketsMain.IS_CLIENT && world.isClientSide()) {
                                TrinketsClient.quickMoveTimer = 20;
                                TrinketsClient.quickMoveGroup = SlotGroup.getEntityGroups(this.owner).get(type.group());
                                if (ref.index() > 0) {
                                    TrinketsClient.quickMoveType = type;
                                } else {
                                    TrinketsClient.quickMoveType = null;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}