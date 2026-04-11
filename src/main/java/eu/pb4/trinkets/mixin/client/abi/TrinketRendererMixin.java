package eu.pb4.trinkets.mixin.client.abi;


import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TrinketRenderer.class)
public interface TrinketRendererMixin {
    @Intrinsic
    default void extractStates(ItemStack stack, TrinketSlotAccess slotReference, EntityModel<? extends LivingEntityRenderState> contextModel,
                               PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, LivingEntityRenderState state,
                               float limbAngle, float limbDistance) {}


    /**
     * @author Patbox
     * @reason Backwards compatibility with extractStates
     */
    @Overwrite
    default void submit(ItemStack stack, TrinketSlotAccess slotReference, EntityModel<? extends LivingEntityRenderState> contextModel,
                PoseStack matrices, SubmitNodeCollector submit, int light, LivingEntityRenderState state,
                float limbAngle, float limbDistance) {
        this.extractStates(stack, slotReference, contextModel, matrices, submit, light, state, limbAngle, limbDistance);
    };
}
