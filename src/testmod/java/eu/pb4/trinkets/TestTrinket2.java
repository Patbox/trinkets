package eu.pb4.trinkets;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.function.BiConsumer;

public class TestTrinket2 extends Item implements TrinketCallback, TrinketRenderer {

	public TestTrinket2(Properties settings) {
		super(settings);
	}

	@Override
	public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier id,
									   BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {		// un-comment to check - testing the composition of the new attribute suffix
		// TrinketsTest.LOGGER.info(TrinketModifiers.toSlotReferencedModifier(new EntityAttributeModifier(id.withSuffixedPath("trinkets-testmod/movement_speed"),
		//		0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot));
		AttributeModifier speedModifier = new AttributeModifier(id.withSuffix("trinkets-testmod/movement_speed"),
				0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		consumer.accept(Attributes.MOVEMENT_SPEED, speedModifier);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void extractStates(ItemStack stack, TrinketSlotAccess slotReference, EntityModel<? extends LivingEntityRenderState> contextModel, PoseStack matrices, SubmitNodeCollector queue, int light, LivingEntityRenderState state, float limbAngle, float limbDistance) {
		if (state instanceof HumanoidRenderState humanoidState && contextModel instanceof HumanoidModel<?> model) {
			var group = slotReference.slotType().group();
			switch (group) {
				case "hand" -> TrinketRenderer.translateToMainHand(matrices, model, humanoidState);
				case "offhand" -> TrinketRenderer.translateToOffHand(matrices, model, humanoidState);
				case "chest" -> TrinketRenderer.translateToChest(matrices, model, humanoidState);
				case "feet", "legs" -> TrinketRenderer.translateToLeftLeg(matrices, model, humanoidState);
				case "head" -> TrinketRenderer.translateToFace(matrices, model, humanoidState, state.yRot, state.xRot);
			}

			matrices.mulPose(new Quaternionf().rotateX(Mth.HALF_PI));
			matrices.scale(0.5f, 0.5f, 0.5f);
			var r = new ItemStackRenderState();
			Minecraft.getInstance().getItemModelResolver().appendItemLayers(r, stack, ItemDisplayContext.FIXED, null, null, 0);
			r.submit(matrices, queue, light, 0, 0);
		}
	}
}