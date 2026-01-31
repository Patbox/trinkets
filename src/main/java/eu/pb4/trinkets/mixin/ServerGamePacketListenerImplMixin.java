package eu.pb4.trinkets.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Allows mutation of trinket slots (and all other valid slots) in the creative inventory
 * 
 * @author Emi
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;
	
	@ModifyConstant(method = "handleSetCreativeModeSlot", constant = @Constant(intValue = 45))
	public int modifyCreativeSlotMax(int value) {
		return player.inventoryMenu.slots.size();
	}
}
