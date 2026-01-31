package eu.pb4.trinkets;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.SlotReference;
import eu.pb4.trinkets.api.TrinketItem;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.client.TrinketModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class TestTrinket extends TrinketItem implements TrinketRenderer {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TrinketsTest.MOD_ID, "textures/entity/trinket/hat.png");
	private HumanoidModel<HumanoidRenderState> model;

	public TestTrinket(Properties settings) {
		super(settings);
	}

	@Override
	public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
		/*stack.damage(1, entity, e -> {
			TrinketsApi.onTrinketBroken(stack, slot, entity);
		});*/
	}

	@Override
	public Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, Identifier id) {
		Multimap<Holder<Attribute>, AttributeModifier> modifiers = super.getModifiers(stack, slot, entity, id);
		AttributeModifier speedModifier = new AttributeModifier(id.withSuffix("trinkets-testmod/movement_speed"),
				0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifiers.put(Attributes.MOVEMENT_SPEED, speedModifier);
		SlotAttributes.addSlotModifier(modifiers, "offhand/ring", id.withSuffix("trinkets-testmod/ring_slot"), 6, AttributeModifier.Operation.ADD_VALUE);
		SlotAttributes.addSlotModifier(modifiers, "hand/glove", id.withSuffix("trinkets-testmod/glove_slot"), 1, AttributeModifier.Operation.ADD_VALUE);
		return modifiers;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntityRenderState> contextModel, PoseStack matrices, SubmitNodeCollector queue, int light, LivingEntityRenderState state, float limbAngle, float limbDistance) {
		if (state instanceof HumanoidRenderState bipedEntityRenderState) {
			HumanoidModel<HumanoidRenderState> model = this.getModel();
			model.setupAnim(bipedEntityRenderState);
			TrinketRenderer.followBodyRotations(contextModel, model);
			queue.submitModel(model, bipedEntityRenderState, matrices, model.renderType(TEXTURE), light, OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(false)), -1, null, state.outlineColor, null);
		}
	}

	@Environment(EnvType.CLIENT)
	private HumanoidModel<HumanoidRenderState> getModel() {
		if (this.model == null) {
			// Vanilla 1.17 uses EntityModels, EntityModelLoader and EntityModelLayers
			this.model = new TrinketModel(TrinketModel.getTexturedModelData().bakeRoot());
		}

		return this.model;
	}
}
