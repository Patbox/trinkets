package eu.pb4.trinkets.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

@Environment(EnvType.CLIENT)
public class TrinketModel extends HumanoidModel<HumanoidRenderState> {

	public TrinketModel(ModelPart root) {
		super(root);
		this.setAllVisible(false);
		this.head.visible = true;
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition modelData = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
		PartDefinition modelPartData = modelData.getRoot();
		PartDefinition head = modelPartData.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-4f, -16f, -4f, 8f, 8f, 8f), PartPose.ZERO);
		head.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 16)
				.addBox(-5f, -9f, -5f, 10f, 1f, 10f), PartPose.ZERO);
		return LayerDefinition.create(modelData, 64, 32);
	}
}
