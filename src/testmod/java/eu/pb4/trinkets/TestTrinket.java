package eu.pb4.trinkets;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

public class TestTrinket extends Item implements TrinketRenderer, TrinketCallback {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TrinketsTest.MOD_ID, "textures/entity/trinket/hat.png");
	private final Holder<Attribute> offhandRingAttribute;
	private final Holder<Attribute> handGloveAttribute;
	private HumanoidModel<HumanoidRenderState> model;

	public TestTrinket(Properties settings) {
		super(settings);

		this.offhandRingAttribute = SlotAttributes.createAttributeForSlot("offhand/ring");
		this.handGloveAttribute = SlotAttributes.createAttributeForSlot("hand/glove");
	}

	@Override
	public void tick(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		if (entity.level() instanceof ServerLevel level) {
			stack.hurtAndBreak(1, level, entity instanceof ServerPlayer player ? player : null, e -> {
				TrinketsApi.onTrinketBroken(stack, slot, entity);
			});
		}
	}

	@Override
	public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier id,
									   BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		AttributeModifier speedModifier = new AttributeModifier(id.withSuffix("trinkets-testmod/movement_speed"),
				0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		consumer.accept(Attributes.MOVEMENT_SPEED, speedModifier);

		consumer.accept(this.offhandRingAttribute, new AttributeModifier(id.withSuffix("trinkets-testmod/ring_slot"), 6, AttributeModifier.Operation.ADD_VALUE));
		consumer.accept(this.handGloveAttribute, new AttributeModifier(id.withSuffix("trinkets-testmod/glove_slot"), 1, AttributeModifier.Operation.ADD_VALUE));
	}


	// Do note, these should be put in their own files to make sure it won't get loaded on server by accidient!
	@Override
	@Environment(EnvType.CLIENT)
	public void submit(ItemStack stack, TrinketSlotAccess slotReference, EntityModel<? extends LivingEntityRenderState> contextModel, PoseStack matrices, SubmitNodeCollector submit, int light, LivingEntityRenderState state, float limbAngle, float limbDistance) {
		if (state instanceof HumanoidRenderState bipedEntityRenderState) {
			HumanoidModel<HumanoidRenderState> model = this.getModel();
			model.setupAnim(bipedEntityRenderState);
			TrinketRenderer.followBodyRotations(contextModel, model);
			submit.submitModel(model, bipedEntityRenderState, matrices, model.renderType(TEXTURE), light, OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(false)), -1, null, state.outlineColor, null);
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
