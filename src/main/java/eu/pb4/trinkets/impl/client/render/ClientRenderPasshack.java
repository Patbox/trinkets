package eu.pb4.trinkets.impl.client.render;

import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ClientRenderPasshack {
    public static final ResourceKey<EquipmentAsset> FAKE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("", "fake_equipment"));
    public static final Optional<ResourceKey<EquipmentAsset>> FAKE_ASSET_OPT = Optional.of(FAKE_ASSET);
    public static Optional<EquipmentClientInfo> replacementEquipmentInfo = Optional.empty();
}
