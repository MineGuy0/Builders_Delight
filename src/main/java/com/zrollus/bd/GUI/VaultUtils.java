package com.zrollus.bd.GUI;

import com.zrollus.bd.Lib.LocationStorageLib;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class VaultUtils {

    public static void saveVault(ServerPlayerEntity player, int vaultNumber, SimpleInventory inventory) {
        saveVaultByUuid(player.getServer(), player.getUuid(), vaultNumber, inventory);
    }

    /**
     * Core logic: Saves vault data using UUID (Works for offline players)
     */
    public static void saveVaultByUuid(MinecraftServer server, UUID uuid, int vaultNumber, SimpleInventory inventory) {
        try {
            NbtCompound nbt = new NbtCompound();
            NbtList nbtList = new NbtList();

            for (int i = 0; i < inventory.size(); i++) {
                ItemStack itemStack = inventory.getStack(i);
                if (!itemStack.isEmpty()) {
                    NbtCompound itemNbt = new NbtCompound();
                    itemNbt.putByte("Slot", (byte) i);
                    itemNbt.copyFrom((NbtCompound) itemStack.encode(server.getRegistryManager()));
                    nbtList.add(itemNbt);
                }
            }

            nbt.put("Items", nbtList);
            PlayerDataModel data = LocationStorageLib.getPlayerData(server, uuid);

            // Convert NBT to String for JSON storage
            data.playerVaults.put(String.valueOf(vaultNumber), nbt.toString());
            LocationStorageLib.savePlayerData(server, uuid, data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SimpleInventory loadVault(ServerPlayerEntity player, int vaultNumber) {
        return loadVaultByUuid(player.getServer(), player.getUuid(), vaultNumber);
    }

    public static SimpleInventory loadVaultByUuid(MinecraftServer server, UUID uuid, int vaultNumber) {
        SimpleInventory inventory = new SimpleInventory(54);
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, uuid);

        if (data == null) return inventory;

        String nbtString = data.playerVaults.get(String.valueOf(vaultNumber));

        if (nbtString != null && !nbtString.isEmpty()) {
            try {
                NbtCompound nbt = StringNbtReader.parse(nbtString);
                NbtList nbtList = nbt.getList("Items", 10); // 10 is the ID for NbtCompound

                for (int i = 0; i < nbtList.size(); i++) {
                    NbtCompound itemNbt = nbtList.getCompound(i);
                    int slot = itemNbt.getByte("Slot") & 255;

                    if (slot < inventory.size()) {
                        inventory.setStack(slot, ItemStack.fromNbtOrEmpty(server.getRegistryManager(), itemNbt));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return inventory;
    }

    public static NbtCompound getOfflinePlayerData(MinecraftServer server, UUID uuid) {
        // Access the 'playerdata' folder in the world save
        File playerDir = server.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        File playerFile = new File(playerDir, uuid.toString() + ".dat");

        if (playerFile.exists()) {
            try {
                // NbtIo handles the GZIP compression Minecraft uses for .dat files
                return NbtIo.readCompressed(playerFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Inside VaultUtils.java
    public static void saveOfflinePlayerData(MinecraftServer server, UUID uuid, Inventory inventory) {
        File playerDir = server.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        File playerFile = new File(playerDir, uuid.toString() + ".dat");

        try {
            NbtCompound nbt = getOfflinePlayerData(server, uuid);
            if (nbt == null) return; // Should not happen if they've played before

            NbtList inventoryList = new NbtList();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    NbtCompound itemNbt = new NbtCompound();
                    // Map our InvSee slot back to Vanilla NBT slot
                    int vanillaSlot = mapInvSeeToVanilla(i);
                    itemNbt.putByte("Slot", (byte) vanillaSlot);
                    itemNbt.copyFrom((NbtCompound) stack.encode(server.getRegistryManager()));
                    inventoryList.add(itemNbt);
                }
            }

            nbt.put("Inventory", inventoryList);
            NbtIo.writeCompressed(nbt, playerFile.toPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SimpleInventory createOfflineInventory(MinecraftServer server, NbtCompound nbt) {
        // 41 slots: 0-35 (Main), 36-39 (Armor), 40 (Offhand)
        SimpleInventory inventory = new SimpleInventory(41);

        if (nbt != null && nbt.contains("Inventory", 9)) { // 9 is NbtList ID
            NbtList nbtList = nbt.getList("Inventory", 10); // 10 is NbtCompound ID

            for (int i = 0; i < nbtList.size(); ++i) {
                NbtCompound itemNbt = nbtList.getCompound(i);
                int vanillaSlot = itemNbt.getByte("Slot") & 255;

                // Map the weird vanilla slot numbers to our 0-40 range
                int targetSlot = mapVanillaToInvSee(vanillaSlot);

                if (targetSlot != -1 && targetSlot < inventory.size()) {
                    inventory.setStack(targetSlot, ItemStack.fromNbtOrEmpty(server.getRegistryManager(), itemNbt));
                }
            }
        }
        return inventory;
    }

    // Logic for LOADING (Vanilla NBT -> Our 0-41 Inventory)
    private static int mapVanillaToInvSee(int vanillaSlot) {
        if (vanillaSlot >= 0 && vanillaSlot <= 35) return vanillaSlot; // Main/Hotbar
        if (vanillaSlot >= 100 && vanillaSlot <= 103) return (vanillaSlot - 100) + 36; // Armor
        if (vanillaSlot == -106 || (vanillaSlot & 255) == 150) return 40; // Offhand
        return -1;
    }

    // Logic for SAVING (Our 0-41 Inventory -> Vanilla NBT)
    private static int mapInvSeeToVanilla(int invSeeSlot) {
        if (invSeeSlot >= 0 && invSeeSlot <= 35) return invSeeSlot; // Main/Hotbar
        if (invSeeSlot == 36) return 100; // Feet
        if (invSeeSlot == 37) return 101; // Legs
        if (invSeeSlot == 38) return 102; // Chest
        if (invSeeSlot == 39) return 103; // Head
        if (invSeeSlot == 40) return -106; // Offhand
        return -1;
    }
}
