package com.zrollus.bd.Lib.libHelpers;

import com.zrollus.bd.Lib.LocationStorageLib;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.UUID;

import com.zrollus.bd.Lib.DynamicIdMapper;
import net.minecraft.util.Formatting;

public class ShopSignData {
    public String owner;
    public int amount;
    public long buyPrice = -1;
    public long sellPrice = -1;
    public String item;
    public String fullItemName;
    public boolean isAdminShop;

    public static ShopSignData from(SignBlockEntity s) {
        try {
            String[] l = new String[4];
            for (int i = 0; i < 4; i++) {
                l[i] = s.getFrontText().getMessage(i, false).getString().trim();
            }
            ShopSignData d = new ShopSignData();
            d.owner = l[0];
            if (d.owner == null || d.owner.isEmpty()) {
                System.out.println("DEBUG ERROR: Owner name is NULL or Empty on the sign!");
            }

            if (d.owner.toLowerCase().equals("admin")) {
                d.isAdminShop = true;
            }

            // 2. SET THE AMOUNT (Line 2) - THIS IS THE FIX
            try {
                // We use l[1] because that is the 2nd line of the sign
                d.amount = Integer.parseInt(l[1]);
                if (d.amount <= 0) d.amount = 1; // Sanity check
            } catch (NumberFormatException e) {
                System.out.println("DEBUG ERROR: Could not parse amount from line: " + l[1]);
                d.amount = 1; // Default to 1 if the line isn't a number
            }
            String priceLine = l[2].toUpperCase().replace(" ", ""); // Remove all spaces for easier parsing
            if (priceLine.contains(":")) {
                String[] parts = priceLine.split(":");
                for (String part : parts) {
                    parsePricePart(d, part);
                }
            } else {
                parsePricePart(d, priceLine);
            }
            // ... (Owner, Amount, Price logic same as before) ...

            // --- ITEM RESOLUTION ---
            NbtCompound nbt = s.createNbt();
            String hiddenId = nbt.getString("ShopItemRaw");
            d.fullItemName = hiddenId;
            String visualLine = l[3]; // "Waxed Exposed"

            if (hiddenId.isEmpty() && l[3].startsWith("#")) {
                try {
                    int id = Integer.parseInt(l[3].substring(1));
                    var server = s.getWorld().getServer();
                    if (server != null) {
                        // Retrieve the FULL string from the .dat file
                        hiddenId = DynamicIdMapper.getServerState(server).getName(id);
                    }
                } catch (Exception ignored) {}
            }

// FINAL CHECK: No guessing! If we don't have a namespace:path, it's not a valid shop.
            if (hiddenId == null || !hiddenId.contains(":")) {
                return null;
            }

            d.item = hiddenId;

            // --- DEBUG BLOCK ---
            // This will tell us if we are using the NBT or the fallback
            if (!hiddenId.isEmpty()) {
                System.out.println("DEBUG: Resolved from NBT: " + d.item);
            } else {
                System.out.println("DEBUG: Resolved from Text: " + d.item);
            }

            return d;
        } catch (Exception e) { return null; }
    }

    private static void parsePricePart(ShopSignData d, String part) {
        try {
            if (part.startsWith("B")) {
                d.buyPrice = Long.parseLong(part.substring(1));
            } else if (part.startsWith("S")) {
                d.sellPrice = Long.parseLong(part.substring(1));
            }
        } catch (NumberFormatException ignored) {
            // Handle cases where B or S isn't followed by a valid number
        }
    }

    public static void transfer(String fromName, String toName, long amount, MinecraftServer server) {
        var userCache = server.getUserCache();
        var toProfile = userCache.findByName(toName);
        if (fromName.equalsIgnoreCase("admin")) {
            UUID toUuid = toProfile.get().getId();
            PlayerDataModel toData = LocationStorageLib.getPlayerData(server, toUuid);
            toData.bal += amount;

            LocationStorageLib.savePlayerData(server, toUuid, toData);
            System.out.println("DEBUG: Transfer Successful: " + amount + " from AdminShop to " + toName);
        }
        else {
            var fromProfile = userCache.findByName(fromName);
            // CRITICAL: Check if BOTH profiles were actually found in the cache
            if (fromProfile.isPresent() && toProfile.isPresent()) {
                UUID fromUuid = fromProfile.get().getId();
                UUID toUuid = toProfile.get().getId();

                // Perform the balance swap in your storage lib
                PlayerDataModel fromData = LocationStorageLib.getPlayerData(server, fromUuid);
                PlayerDataModel toData = LocationStorageLib.getPlayerData(server, toUuid);

                if (fromData.bal >= amount) {
                    fromData.bal -= amount;
                    toData.bal += amount;

                    LocationStorageLib.savePlayerData(server, fromUuid, fromData);
                    LocationStorageLib.savePlayerData(server, toUuid, toData);
                    System.out.println("DEBUG: Transfer Successful: " + amount + " from " + fromName + " to " + toName);
                }
            } else {
                // Log exactly which name failed to resolve
                if (fromProfile.isEmpty()) System.out.println("DEBUG: Transfer Failed - Could not find UUID for buyer: " + fromName);
                if (toProfile.isEmpty()) System.out.println("DEBUG: Transfer Failed - Could not find UUID for owner: " + toName);
            }
        }
    }
}