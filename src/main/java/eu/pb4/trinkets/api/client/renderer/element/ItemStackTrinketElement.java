package eu.pb4.trinkets.api.client.renderer.element;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.client.renderer.AttachmentSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
public record ItemStackTrinketElement(AttachmentSettings settings, ItemDisplayContext displayContext) implements TrinketRenderElement {
    public static final MapCodec<ItemStackTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentSettings.CODEC.forGetter(ItemStackTrinketElement::settings),
            ItemDisplayContext.CODEC.optionalFieldOf("display_context", ItemDisplayContext.NONE).forGetter(ItemStackTrinketElement::displayContext)
    ).apply(instance, ItemStackTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> type() {
        return CODEC;
    }

    @Override
    public Baked bake(BakingContext bakingContext) {
        return (owner, item, access, level, context, state) -> {
            var itemStackState = new ItemStackRenderState();
            context.itemModelResolver().updateForLiving(itemStackState, item, this.displayContext, owner);
            context.setupAttachedRenderer(this.settings.withResolvedModelPart(owner, access), itemStackState::submit);
        };
    }
}
