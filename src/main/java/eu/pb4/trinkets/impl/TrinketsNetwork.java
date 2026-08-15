package eu.pb4.trinkets.impl;

import eu.pb4.trinkets.impl.payload.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class TrinketsNetwork {

  // Server to Client (Clientbound)
  public static final CustomPacketPayload.Type<SyncSlotsPayload> SYNC_SLOTS = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "sync_slots"));
  public static final CustomPacketPayload.Type<SyncInventoryPayload> SYNC_INVENTORY = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "sync_inventory"));
  public static final CustomPacketPayload.Type<BreakPayload> BREAK = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "break"));
  public static final CustomPacketPayload.Type<SyncConfigPayload> SYNC_CONFIG = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "sync_config"));

  // Client to Server (Serverbound)
  public static final CustomPacketPayload.Type<ToggleVisibilityPayload> TOGGLE_VISIBILITY = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "toggle_visibility"));
  public static final CustomPacketPayload.Type<ToggleCosmeticModePayload> TOGGLE_COSMETIC_MODE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "toggle_cosmetic"));

  private TrinketsNetwork() {
  }
}
