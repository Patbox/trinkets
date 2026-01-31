package eu.pb4.trinkets.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import eu.pb4.trinkets.api.SlotReference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public interface TrinketRenderer {

	/**
	 * Renders the Trinket
	 *
	 * @param stack The {@link ItemStack} for the Trinket being rendered
	 * @param slotReference The exact slot for the item being rendered
	 * @param contextModel The model this Trinket is being rendered on
	 */
	void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntityRenderState> contextModel,
				PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, LivingEntityRenderState state,
				float limbAngle, float limbDistance);

	/**
	 * Rotates the rendering for the models based on the entity's poses and movements. This will do
	 * nothing if the entity render object does not implement {@link LivingEntityRenderer} or if the
	 * model does not implement {@link HumanoidModel}).
	 *
	 * @param entityModel The model of wearer of the trinket
	 * @param model The model to align to the body movement
	 */
	@SuppressWarnings("unchecked")
	static void followBodyRotations(final EntityModel<? extends LivingEntityRenderState> entityModel, final HumanoidModel<?> model) {
		if (entityModel instanceof HumanoidModel<?> bipedModel) {
			//noinspection rawtypes
			model.head.loadPose(bipedModel.head.storePose());
			model.body.loadPose(bipedModel.body.storePose());
			model.leftArm.loadPose(bipedModel.leftArm.storePose());
			model.rightArm.loadPose(bipedModel.rightArm.storePose());
			model.leftLeg.loadPose(bipedModel.leftLeg.storePose());
			model.rightLeg.loadPose(bipedModel.rightLeg.storePose());
		}
	}

	/**
	 * Translates the rendering context to the center of the player's face
	 */
	static void translateToFace(PoseStack matrices, HeadedModel model,
			HumanoidRenderState state, float headYaw, float headPitch) {

		if (state.isVisuallySwimming || state.isFallFlying) {
			matrices.mulPose(Axis.ZP.rotationDegrees(model.getHead().zRot));
			matrices.mulPose(Axis.YP.rotationDegrees(headYaw));
			matrices.mulPose(Axis.XP.rotationDegrees(-45.0F));
		} else {

			if (state.isCrouching || state.hasPose(Pose.SITTING)) {
				matrices.translate(0.0F, 0.25F, 0.0F);
			}
			matrices.mulPose(Axis.YP.rotationDegrees(headYaw));
			matrices.mulPose(Axis.XP.rotationDegrees(headPitch));
		}
		matrices.translate(0.0F, -0.25F, -0.3F);
	}

	/**
	 * Translates the rendering context to the center of the player's chest/torso segment
	 */
	static void translateToChest(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {

		if (state.isCrouching && !state.hasPose(Pose.SITTING) && !state.isVisuallySwimming) {
			matrices.translate(0.0F, 0.2F, 0.0F);
			matrices.mulPose(Axis.XP.rotation(model.body.xRot));
		}
		matrices.mulPose(Axis.YP.rotation(model.body.yRot));
		matrices.translate(0.0F, 0.4F, -0.16F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right arm
	 */
	static void translateToRightArm(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {

		if (state.isCrouching && !state.hasPose(Pose.SITTING) && !state.isVisuallySwimming) {
			matrices.translate(0.0F, 0.2F, 0.0F);
		}
		matrices.mulPose(Axis.YP.rotation(model.body.yRot));
		matrices.translate(-0.3125F, 0.15625F, 0.0F);
		matrices.mulPose(Axis.ZP.rotation(model.rightArm.zRot));
		matrices.mulPose(Axis.YP.rotation(model.rightArm.yRot));
		matrices.mulPose(Axis.XP.rotation(model.rightArm.xRot));
		matrices.translate(-0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left arm
	 */
	static void translateToLeftArm(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {

		if (state.isCrouching && !state.hasPose(Pose.SITTING) && !state.isVisuallySwimming) {
			matrices.translate(0.0F, 0.2F, 0.0F);
		}
		matrices.mulPose(Axis.YP.rotation(model.body.yRot));
		matrices.translate(0.3125F, 0.15625F, 0.0F);
		matrices.mulPose(Axis.ZP.rotation(model.leftArm.zRot));
		matrices.mulPose(Axis.YP.rotation(model.leftArm.yRot));
		matrices.mulPose(Axis.XP.rotation(model.leftArm.xRot));
		matrices.translate(0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right leg
	 */
	static void translateToRightLeg(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {

		if (state.isCrouching && !state.hasPose(Pose.SITTING) && !state.isVisuallySwimming) {
			matrices.translate(0.0F, 0.0F, 0.25F);
		}
		matrices.translate(-0.125F, 0.75F, 0.0F);
		matrices.mulPose(Axis.ZP.rotation(model.rightLeg.zRot));
		matrices.mulPose(Axis.YP.rotation(model.rightLeg.yRot));
		matrices.mulPose(Axis.XP.rotation(model.rightLeg.xRot));
		matrices.translate(0.0F, 0.75F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left leg
	 */
	static void translateToLeftLeg(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {
		if (state.isCrouching && !state.hasPose(Pose.SITTING) && !state.isVisuallySwimming) {
			matrices.translate(0.0F, 0.0F, 0.25F);
		}
		matrices.translate(0.125F, 0.75F, 0.0F);
		matrices.mulPose(Axis.ZP.rotation(model.leftLeg.zRot));
		matrices.mulPose(Axis.YP.rotation(model.leftLeg.yRot));
		matrices.mulPose(Axis.XP.rotation(model.leftLeg.xRot));
		matrices.translate(0.0F, 0.75F, 0.0F);
	}
}
