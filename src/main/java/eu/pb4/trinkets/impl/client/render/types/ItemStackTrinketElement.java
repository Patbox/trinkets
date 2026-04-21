package eu.pb4.trinkets.impl.client.render.types;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public record ItemStackTrinketElement(AttachmentSettings settings, ItemDisplayContext displayContext) implements TrinketRenderElement {
    public static final MapCodec<ItemStackTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentSettings.CODEC.forGetter(ItemStackTrinketElement::settings),
            ItemDisplayContext.CODEC.optionalFieldOf("display_context", ItemDisplayContext.NONE).forGetter(ItemStackTrinketElement::displayContext)
    ).apply(instance, ItemStackTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> codec() {
        return CODEC;
    }

    @Override
    public void apply(LivingEntity livingEntity, ItemStack stack, TrinketSlotAccess access, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
        var itemStackState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(itemStackState, stack, this.displayContext, livingEntity);
        state.trinkets$getPartAttachedRenderers().add(
                new TrinketEntityRenderState.PartAttachedRenderer(this.settings.withResolvedModelPart(livingEntity, access), itemStackState::submit));
    }
}
