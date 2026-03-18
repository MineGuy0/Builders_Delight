package com.zrollus.bd.Lib;

import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import com.zrollus.bd.Lib.libHelpers.ShopSignData;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.UUID;

public class ShopProcessor {

    public static void processBuy(ServerPlayerEntity customer, ShopSignData shop, Inventory chestInv) {
        if (shop.buyPrice < 0) return;
        MinecraftServer server = customer.getServer();
        System.out.println("DEBUG: Processing buy for " + shop.item);

        String itemName = shop.item; // This is "bd:pokedollar" from your .dat file

// 1. Create the Identifier directly from the full string
        Identifier id = Identifier.tryParse(itemName);
        if (id == null) {
            customer.sendMessage(Text.literal("Shop » Invalid Item ID!").formatted(Formatting.RED));
            return;
        }

// 2. Get the item from the registry using that ID
        Item item = Registries.ITEM.get(id);
        // 2. Safety Check: If the item is Air, the ID was invalid
        if (item == Items.AIR) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("Error: Item '" + shop.item + "' not found in registry!").formatted(Formatting.RED)), false);
            return;
        }

        PokedollarHandler.syncPhysicalToVirtual(customer);
        var customerData = LocationStorageLib.getPlayerData(server, customer.getUuid());

        if (customerData.bal < shop.buyPrice) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("You need ₱" + (shop.buyPrice - customerData.bal) + " more!").formatted(Formatting.RED)), false);
            return;
        }

        if (countItems(chestInv, item) < shop.amount) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("This shop is out of stock!").formatted(Formatting.RED)), false);
            return;
        }

        if (shop.isAdminShop) {
            // 1. Check if player has enough money
            if (customerData.bal >= shop.buyPrice) {
                // 2. Take money (into the void)
                ShopSignData.transfer(customer.getName().getString(), "Admin", shop.sellPrice, server);
                // 3. Give item (from thin air)
                customer.giveItemStack(new ItemStack(Registries.ITEM.get(new Identifier(shop.item)), shop.amount));
                customer.sendMessage(Text.literal("Bought from Server!").formatted(Formatting.GREEN), true);
            }
        }
        System.out.println("DEBUG: Chest Items found: " + countItems(chestInv, item));
        System.out.println("DEBUG: Customer Balance: " + customerData.bal);

        removeItems(chestInv, item, shop.amount);
        customer.getInventory().offerOrDrop(new ItemStack(item, shop.amount));
        System.out.println("DEBUG: Shop Stock:  " + shop.amount);

        // Process Economy
        ShopSignData.transfer(customer.getName().getString(), shop.owner, shop.buyPrice, server);

        // 3. User Feedback
        customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                .append(Text.literal("You bought ").formatted(Formatting.WHITE))
                .append(Text.literal(shop.amount + "x ").formatted(Formatting.YELLOW))
                .append(Text.literal(item.getName().getString()).formatted(Formatting.YELLOW)) // Use official name
                .append(Text.literal(" for ").formatted(Formatting.WHITE))
                .append(Text.literal("₱" + shop.buyPrice).formatted(Formatting.GREEN)), false);
    }

    public static void processSell(ServerPlayerEntity customer, ShopSignData shop, Inventory chestInv) {
        if (shop.sellPrice < 0) return;
        MinecraftServer server = customer.getServer();

        // 1. SANITIZE THE ITEM ID (The Fix for your crash)
        String sanitizedItem = shop.item.toLowerCase().trim().replace(" ", "_");
        if (!sanitizedItem.contains(":")) {
            sanitizedItem = "minecraft:" + sanitizedItem;
        }

        Identifier id = new Identifier(sanitizedItem);
        Item item = Registries.ITEM.get(id);

        // 2. SAFETY CHECK: Ensure item exists
        if (item == Items.AIR) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("Error: Item '" + shop.item + "' not found.").formatted(Formatting.RED)), false);
            return;
        }

        var ownerProfile = server.getUserCache().findByName(shop.owner);
        if (ownerProfile.isEmpty()) return;
        var ownerData = LocationStorageLib.getPlayerData(server, ownerProfile.get().getId());

        // 3. CHECK ECONOMY
        if (ownerData.bal < shop.sellPrice) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("The shop owner is out of money!").formatted(Formatting.RED)), false);
            return;
        }

        // 4. CHECK CUSTOMER INVENTORY
        if (countItems(customer.getInventory(), item) < shop.amount) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("You don't have enough items to sell!").formatted(Formatting.RED)), false);
            return;
        }

        // 5. CHECK CHEST SPACE (Prevent items from being deleted if chest is full)
        if (!hasSpace(chestInv, item, shop.amount)) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("This shop's chest is full!").formatted(Formatting.RED)), false);
            return;
        }

        if (shop.isAdminShop) {
            // 1. Check if player has the item
            if (countItems(customer.getInventory(), item) > 0) {
                // 2. Remove from player
                removeItems(customer.getInventory(), item, shop.amount);
                // 3. Add money to player (from thin air)
                ShopSignData.transfer("Admin", customer.getName().getString(), shop.sellPrice, server);
                customer.sendMessage(Text.literal("Sold to Server!").formatted(Formatting.GREEN), true);
            }
        }

        // Perform Transaction
        removeItems(customer.getInventory(), item, shop.amount);
        // 1. Check if player has the items (Standard Minecraft Inventory check)
        int count = 0;
        for (int i = 0; i < customer.getInventory().size(); i++) {
            ItemStack stack = customer.getInventory().getStack(i);
            if (stack.getItem() == item) count += stack.getCount();
        }

        if (count < shop.amount) {
            customer.sendMessage(Text.literal("§cYou don't have enough " + item.getName().getString() + "!"), true);
            return;
        }

        // Transfer money (Note: false usually indicates a Sell/Withdrawal from owner)
        ShopSignData.transfer(shop.owner, customer.getName().getString(), shop.sellPrice, server);

        customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                .append(Text.literal("You sold ").formatted(Formatting.WHITE))
                .append(Text.literal(shop.amount + "x ").formatted(Formatting.YELLOW))
                .append(Text.literal(item.getName().getString()).formatted(Formatting.YELLOW))
                .append(Text.literal(" for ").formatted(Formatting.WHITE))
                .append(Text.literal("₱" + shop.sellPrice).formatted(Formatting.GREEN)), false);
    }

    public static int countItems(Inventory inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(item)) count += inv.getStack(i).getCount();
        }
        return count;
    }

    public static void removeItems(Inventory inv, Item item, int amount) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) {
                int rem = Math.min(stack.getCount(), amount);
                stack.decrement(rem);
                amount -= rem;
                if (amount <= 0) return;
            }
        }
    }

    private static boolean hasSpace(Inventory inv, Item item, int amount) {
        int freeSpace = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                freeSpace += item.getMaxCount();
            } else if (stack.isOf(item)) {
                freeSpace += (item.getMaxCount() - stack.getCount());
            }
            if (freeSpace >= amount) return true;
        }
        return false;
    }


}