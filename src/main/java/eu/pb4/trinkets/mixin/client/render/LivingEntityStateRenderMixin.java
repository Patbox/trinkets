package eu.pb4.trinkets.mixin.client.render;

import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.EnumMap;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderState.class)
public class LivingEntityStateRenderMixin implements TrinketEntityRenderState {
    @Unique
    private List<Tuple<ItemStack, TrinketSlotAccess>> trinketsState = List.of();
    @Unique
    private List<TrinketEntityRenderState.PartAttachedRenderer> partAttachedRenderers = List.of();

    @Unique
    private final EnumMap<EquipmentSlot, EquipmentOverride> equipmentOverride = new EnumMap<>(EquipmentSlot.class);
    private EquipmentOverride wingsOverride;

    @Override
    public void trinkets$setItems(List<Tuple<ItemStack, TrinketSlotAccess>> items) {
        this.trinketsState = items;
    }

    @Override
    public void trinkets$setPartAttachedRenderers(List<PartAttachedRenderer> items) {
        this.partAttachedRenderers = items;
    }

    @Override
    public List<Tuple<ItemStack, TrinketSlotAccess>> trinkets$getItems() {
        return this.trinketsState;
    }

    @Override
    public List<PartAttachedRenderer> trinkets$getPartAttachedRenderers() {
        return this.partAttachedRenderers;
    }

    @Override
    public void trinkets$setEquipmentOverride(EquipmentSlot slot, EquipmentOverride override) {
        this.equipmentOverride.put(slot, override);
    }

    @Override
    public @Nullable EquipmentOverride trinkets$getEquipmentOverride(EquipmentSlot slot) {
        return this.equipmentOverride.get(slot);
    }

    @Override
    public void trinkets$setWingOverride(EquipmentOverride override) {
        this.wingsOverride = override;
    }

    @Override
    public @Nullable EquipmentOverride trinkets$getWingOverride() {
        return this.wingsOverride;
    }
}
