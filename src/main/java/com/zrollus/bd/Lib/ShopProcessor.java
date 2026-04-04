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
            customer.sendMessage(MessageLib.shopOutofMoney(shop.buyPrice - customerData.bal), false);
            return;
        }

        if (!shop.owner.equalsIgnoreCase(customer.getName().getString())) {
            if (shop.isAdminShop) {
                // 1. Check if player has enough money
                if (customerData.bal >= shop.buyPrice) {
                    // 2. Take money (into the void)
                    ShopSignData.transfer(customer.getName().getString(), "Admin", shop.buyPrice, server);
                    // 3. Give item (from thin air)
                    customer.giveItemStack(new ItemStack(Registries.ITEM.get(new Identifier(shop.item)), shop.amount));
                    customer.sendMessage(Text.literal("Bought from Server!").formatted(Formatting.GREEN), true);
                }
            }
            else {
                if (countItems(chestInv, item) < shop.amount) {
                    customer.sendMessage(MessageLib.SHOP_OUT_OF_STOCK, false);
                    return;
                }
            }

            removeItems(chestInv, item, shop.amount);
            customer.getInventory().offerOrDrop(new ItemStack(item, shop.amount));

            // Process Economy
            ShopSignData.transfer(customer.getName().getString(), shop.owner, shop.buyPrice, server);

            // 3. User Feedback
            customer.sendMessage(MessageLib.shopSuccess("bought", shop.amount, item.getName().getString(), shop.buyPrice), false);
        }
        else {
            customer.sendMessage(MessageLib.SHOP_OWNER_SELF, false);
        }

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

        // 4. CHECK CUSTOMER INVENTORY
        if (countItems(customer.getInventory(), item) < shop.amount) {
            customer.sendMessage(Text.literal("Shop » ").formatted(Formatting.GOLD)
                    .append(Text.literal("You don't have enough items to sell!").formatted(Formatting.RED)), false);
            return;
        }

        if (!shop.owner.equalsIgnoreCase(customer.getName().getString())) {
            if (!shop.isAdminShop) {
                if (ownerData.bal < shop.sellPrice) {
                    customer.sendMessage(MessageLib.SHOP_OWNER_BROKE, false);
                    return;
                }

                if (!hasSpace(chestInv, item, shop.amount)) {
                    customer.sendMessage(MessageLib.SHOP_CHEST_FULL, false);
                    return;
                }
                addItems(chestInv, item, shop.amount);
                ShopSignData.transfer(shop.owner, customer.getName().getString(), shop.sellPrice, server);
            }
            else {
                ShopSignData.transfer("Admin", customer.getName().getString(), shop.sellPrice, server);
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
            customer.sendMessage(MessageLib.shopSuccess("sold", shop.amount, item.getName().getString(), shop.sellPrice), false);
        }
        else {
            customer.sendMessage(Text.literal("Can't sell items to your own shop dingus!").formatted(Formatting.RED));
        }
    }

    public static int countItems(Inventory inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(item)) count += inv.getStack(i).getCount();
        }
        return count;
    }

    public static void addItems(Inventory inv, Item item, int amount) {
        int remaining = amount;

        // First pass: try to stack with existing items
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item) && stack.getCount() < stack.getMaxCount()) {
                int canAdd = Math.min(remaining, stack.getMaxCount() - stack.getCount());
                stack.increment(canAdd);
                remaining -= canAdd;
            }
            if (remaining <= 0) return;
        }

        // Second pass: put remaining in empty slots
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isEmpty()) {
                int canAdd = Math.min(remaining, item.getMaxCount());
                inv.setStack(i, new ItemStack(item, canAdd));
                remaining -= canAdd;
            }
            if (remaining <= 0) return;
        }
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