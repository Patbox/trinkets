package eu.pb4.trinkets;

import eu.pb4.trinkets.api.BuiltInTrinketConditions;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.datagen.TrinketsDataProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.resources.Identifier;

public class DataGenTest implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();

        pack.addProvider((a, b) -> new TrinketsDataProvider(a, b) {
            @Override
            protected void generate(TrinketsOutput output) {
                output.defineSlotGroup("datagen", 5);
                output.slotType("datagen/test_1").amount(1)
                        .icon(Identifier.withDefaultNamespace("container/slot/hoe"))
                        .isVanityOnly(true)
                        .order(16)
                        .validatorCondition(x -> x.orAnyOf(BuiltInTrinketConditions.ALL))
                        .tooltipCondition(x -> x.andAllOf(BuiltInTrinketConditions.NONE))
                        .dropRule(TrinketDropRule.KEEP)
                ;
                output.slotType("datagen/test_2").amount(1)
                        .icon(Identifier.withDefaultNamespace("container/slot/pickaxe"))
                        .order(0)
                        .validatorCondition(x -> x.orAnyOf(BuiltInTrinketConditions.ALL))
                        .tooltipCondition(x -> x.andAllOf(BuiltInTrinketConditions.NONE))
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
    }
}
