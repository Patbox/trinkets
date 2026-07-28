package eu.pb4.trinkets.api.client.renderer.element;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.client.renderer.AttachmentSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.BlockItemStateProperties;

@Environment(EnvType.CLIENT)
public record ItemBlockStateTrinketElement(AttachmentSettings settings) implements TrinketRenderElement {
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    public static final MapCodec<ItemBlockStateTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentSettings.CODEC.forGetter(ItemBlockStateTrinketElement::settings)
    ).apply(instance, ItemBlockStateTrinketElement::new));

    @Override
    public MapCodec<? extends TrinketRenderElement> type() {
        return CODEC;
    }

    @Override
    public Baked bake(BakingContext bakingContext) {
        return (owner, item, access, level, context, state) -> {
            var blockModelRenderState = new BlockModelRenderState();

            if (item.getItem() instanceof BlockItem blockItem) {
                var blockState = item.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(blockItem.getBlock().defaultBlockState());
                context.blockModelResolver().update(blockModelRenderState, blockState, BLOCK_DISPLAY_CONTEXT);
                context.setupAttachedRenderer(this.settings.withResolvedModelPart(owner, access), blockModelRenderState::submit);
            }
        };
    }
}
