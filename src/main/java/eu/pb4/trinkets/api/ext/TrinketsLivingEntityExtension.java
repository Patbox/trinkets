package eu.pb4.trinkets.api.ext;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface TrinketsLivingEntityExtension {
    default TrinketAttachment getTrinkets() {
        return TrinketsApi.getAttachment((LivingEntity) this);
    }
}
