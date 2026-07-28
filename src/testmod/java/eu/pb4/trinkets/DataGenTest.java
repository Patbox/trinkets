package eu.pb4.trinkets;

import eu.pb4.trinkets.api.BuiltInTrinketConditions;
import eu.pb4.trinkets.api.DefaultTrinketSlots;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.client.datagen.ClientTrinketsProvider;
import eu.pb4.trinkets.api.client.renderer.AttachmentSettings;
import eu.pb4.trinkets.api.client.renderer.element.ItemBlockStateTrinketElement;
import eu.pb4.trinkets.api.client.renderer.element.ItemStackTrinketElement;
import eu.pb4.trinkets.api.client.renderer.element.collection.IfSlotTrinketElement;
import eu.pb4.trinkets.api.datagen.TrinketsDataProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

import java.util.List;

public class DataGenTest implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();

        pack.addProvider((a, b) -> new TrinketsDataProvider(a, b) {
            @Override
            protected void generate(TrinketsOutput output) {
                output.defineSlotGroup("datagen", 5);
                output.slotType("datagen/test_1")
                        .amount(1)
                        .maxStackSize(99)
                        .icon(Identifier.withDefaultNamespace("container/slot/hoe"))
                        .isVanityOnly(true)
                        .order(16)
                        .validatorCondition(x -> x.orAnyOf(BuiltInTrinketConditions.ALL))
                        .tooltipCondition(x -> x.andAllOf(BuiltInTrinketConditions.NONE))
                        .interactEquipableCondition(x -> x.andAllOf(BuiltInTrinketConditions.NONE))
                        .dropRule(TrinketDropRule.KEEP)
                ;
                output.slotType("datagen/test_2")
                        .amount(1)
                        .maxStackSize(99)
                        .icon(Identifier.withDefaultNamespace("container/slot/pickaxe"))
                        .order(0)
                        .validatorCondition(x -> x.orAnyOf(BuiltInTrinketConditions.ALL))
                        .tooltipCondition(x -> x.andAllOf(BuiltInTrinketConditions.NONE))
                        .interactEquipableCondition(x -> x.andAllOf(BuiltInTrinketConditions.NONE))
                        .dropRule(TrinketDropRule.DESTROY)
                ;

                output.entitySlots("datagen_entity")
                        .addPlayer()
                        .addSlot("datagen/test_1")
                        .addSlot("datagen/test_2")
                ;
            }

            @Override
            public String getName() {
                return "Trinkets Test Datagen";
            }
        });

        pack.addProvider((output, _) -> new ClientTrinketsProvider(output) {
            @Override
            public String getName() {
                return "Trinkets TestClient Datagen";
            }

            @Override
            protected void generate(ClientTrinketsOutput output) {
                output.acceptClientTrinket(BlockItemIds.COPPER_LANTERN.waxed().exposed().item(), List.of(
                        new IfSlotTrinketElement(DefaultTrinketSlots.HAND_RING, List.of(
                                new ItemStackTrinketElement(
                                        AttachmentSettings.builder(PartNames.RIGHT_ARM).offset(0, -1, 0).build(),
                                        ItemDisplayContext.NONE
                                )
                        ), List.of()),
                        new IfSlotTrinketElement(DefaultTrinketSlots.LEGS_BELT, List.of(
                                new ItemBlockStateTrinketElement(
                                        AttachmentSettings.builder(PartNames.LEFT_LEG).offset(1, 1, 1)
                                                .transformation(new Matrix4f().translation((7 - 8) / 16f,  (2 - 8) / 16f, (-6) / 16f)
                                                        .rotateZ(Mth.DEG_TO_RAD * 30)
                                                        .scale(0.5f)
                                                ).build()
                                )
                        ), List.of())
                ));
            }
        });
    }
}
