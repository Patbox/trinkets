package eu.pb4.trinkets.impl;

import eu.pb4.trinkets.impl.payload.BreakPayload;
import eu.pb4.trinkets.impl.payload.SyncInventoryPayload;
import eu.pb4.trinkets.impl.payload.SyncSlotsPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class TrinketsNetwork {

  public static final CustomPacketPayload.Type<SyncSlotsPayload> SYNC_SLOTS = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "sync_slots"));
  public static final CustomPacketPayload.Type<SyncInventoryPayload> SYNC_INVENTORY = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "sync_inventory"));
  public static final CustomPacketPayload.Type<BreakPayload> BREAK = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TrinketsMain.MOD_ID, "break"));

  private TrinketsNetwork() {
  }
}
