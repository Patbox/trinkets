package eu.pb4.trinkets.impl;

import eu.pb4.trinkets.impl.payload.BreakPayload;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import eu.pb4.trinkets.impl.payload.SyncSlotsPayload;
import eu.pb4.trinkets.impl.payload.ToggleVisibilityPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class TrinketsNetwork {

  // Server to Client (Clientbound)
  public static final CustomPacketPayload.Type<SyncSlotsPayload> SYNC_SLOTS = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "sync_slots"));
  public static final CustomPacketPayload.Type<SyncInventoryPayload> SYNC_INVENTORY = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "sync_inventory"));
  public static final CustomPacketPayload.Type<BreakPayload> BREAK = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "break"));

  // Client to Server (Serverbound)
  public static final CustomPacketPayload.Type<ToggleVisibilityPayload> TOGGLE_VISIBILITY = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "toggle_visibility"));

  private TrinketsNetwork() {
  }
}
