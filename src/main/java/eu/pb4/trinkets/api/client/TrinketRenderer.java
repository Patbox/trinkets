package eu.pb4.trinkets.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.impl.client.render.TrinketRenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public interface TrinketRenderer {

	/**
	 * Renders the Trinket
	 *
	 * @param stack The {@link ItemStack} for the Trinket being rendered
	 * @param slotReference The exact slot for the item being rendered
	 * @param contextModel The model this Trinket is being rendered on
	 */
	void submit(ItemStack stack, TrinketSlotAccess slotReference, EntityModel<? extends LivingEntityRenderState> contextModel,
				PoseStack matrices, SubmitNodeCollector submit, int light, LivingEntityRenderState state,
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
			model.root().loadPose(bipedModel.root().storePose());
			model.head.loadPose(bipedModel.head.storePose());
			model.body.loadPose(bipedModel.body.storePose());
			model.leftArm.loadPose(bipedModel.leftArm.storePose());
			model.rightArm.loadPose(bipedModel.rightArm.storePose());
			model.leftLeg.loadPose(bipedModel.leftLeg.storePose());
			model.rightLeg.loadPose(bipedModel.rightLeg.storePose());
		}
	}

	/**
	 * Translates the rendering to select positions of the select model part, similarly to item/model data driven renderer.
	 *
	 * @param poseStack poseStack to translate
	 * @param model model to align with
	 * @param modelPart name of the model part to target, see {@link PartNames} for vanilla names.
	 * @param offset controls the position alongside the model part, taking values from (-1, -1, -1) to (1, 1, 1)
	 * @return true if modelPart was found and applied correctly, false otherwise
	 */
	static boolean translateToModelPart(PoseStack poseStack, Model<?> model, String modelPart, Vector3fc offset) {
		return TrinketRenderLayer.translateToModelPart(model, modelPart, offset, poseStack);
	}

	static void translateToFace(PoseStack matrices, HeadedModel model) {
		model.translateToHead(matrices);
		matrices.translate(0.0F, -0.25F, -0.3F);
	}

	static void translateToHead(PoseStack matrices, HeadedModel model) {
		model.translateToHead(matrices);
		matrices.translate(0.0F, -0.25F, 0);
	}

	/**
	 * Translates the rendering context to the center of the player's face
	 */
	@Deprecated
	static void translateToFace(PoseStack matrices, HeadedModel model,
								HumanoidRenderState state, float headYaw, float headPitch) {
		translateToFace(matrices, model);
	}

	/**
	 * Translates the rendering context to the center of the player's chest/torso segment
	 */
	static void translateToChest(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {
		model.root().translateAndRotate(matrices);
		model.body.translateAndRotate(matrices);
		matrices.translate(0.0F, 0.4F, -0.16F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right arm
	 */
	static void translateToRightArm(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {
		model.translateToHand(state, HumanoidArm.RIGHT, matrices);
		matrices.translate(-0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's main arm
	 * @return True if main hand is right, false otherwise
	 */
	static boolean translateToMainHand(PoseStack matrices, HumanoidModel<?> model,
									   HumanoidRenderState state) {
		if (state.mainArm == HumanoidArm.LEFT) {
			translateToLeftArm(matrices, model, state);
			return false;
		} else {
			translateToRightArm(matrices, model, state);
			return true;
		}
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's offhand
	 * @return True if main hand is right, false otherwise
	 */
	static boolean translateToOffHand(PoseStack matrices, HumanoidModel<?> model,
									  HumanoidRenderState state) {
		if (state.mainArm == HumanoidArm.LEFT) {
			translateToRightArm(matrices, model, state);
			return false;
		} else {
			translateToLeftArm(matrices, model, state);
			return true;
		}
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left arm
	 */
	static void translateToLeftArm(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {
		model.translateToHand(state, HumanoidArm.LEFT, matrices);
		matrices.translate(0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right leg
	 */
	static void translateToRightLeg(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {
		model.root().translateAndRotate(matrices);
		model.rightLeg.translateAndRotate(matrices);
		matrices.translate(0.0F, 0.75F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left leg
	 */
	static void translateToLeftLeg(PoseStack matrices, HumanoidModel<?> model,
			HumanoidRenderState state) {
		model.root().translateAndRotate(matrices);
		model.leftLeg.translateAndRotate(matrices);
		matrices.translate(0.0F, 0.75F, 0.0F);
	}
}
