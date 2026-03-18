package com.zrollus.bd.Lib;

import com.zrollus.bd.Lib.libHelpers.ShopSignData;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ShopUtils {

    private void processTransaction(ServerPlayerEntity customer, ShopSignData shop, Inventory chestInv) {
        MinecraftServer server = customer.getServer();
        Item tradeItem = Registries.ITEM.get(new Identifier(shop.item));

        // Step 1: Sync physical money to virtual before checking balance
        PokedollarHandler.syncPhysicalToVirtual(customer);
        var customerData = LocationStorageLib.getPlayerData(server, customer.getUuid());

        if (shop.buyPrice > 0) { // Customer buying from chest
            if (customerData.bal < shop.buyPrice) {
                customer.sendMessage(Text.literal("Insufficient funds!").formatted(Formatting.RED));
                return;
            }
            if (countItems(chestInv, tradeItem) < shop.amount) {
                customer.sendMessage(Text.literal("Out of stock!").formatted(Formatting.RED));
                return;
            }

            // Execute Trade
            removeItems(chestInv, tradeItem, shop.amount);
            customer.getInventory().offerOrDrop(new ItemStack(tradeItem, shop.amount));
            ShopSignData.transfer(shop.owner, customer.getName().getString(), shop.buyPrice, server);
            customer.sendMessage(Text.literal("Bought " + shop.amount + "x " + shop.item).formatted(Formatting.GREEN));

        } else { // Customer selling to chest
            if (countItems(customer.getInventory(), tradeItem) < shop.amount) {
                customer.sendMessage(Text.literal("You don't have enough items!").formatted(Formatting.RED));
                return;
            }

            // Ensure owner can afford it (Optional check)
            var ownerProfile = server.getUserCache().findByName(shop.owner);
            if (ownerProfile.isPresent()) {
                var ownerData = LocationStorageLib.getPlayerData(server, ownerProfile.get().getId());
                if (ownerData.bal < shop.buyPrice) {
                    customer.sendMessage(Text.literal("Shop owner is broke!").formatted(Formatting.RED));
                    return;
                }
            }

            // Execute Trade
            removeItems(customer.getInventory(), tradeItem, shop.amount);
            addItems(chestInv, tradeItem, shop.amount);
            ShopSignData.transfer(shop.owner, customer.getName().getString(), shop.buyPrice, server);
            customer.sendMessage(Text.literal("Sold " + shop.amount + "x " + shop.item).formatted(Formatting.GREEN));
        }
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

    public static void addItems(Inventory inv, Item item, int amount) {
        // Logic to find empty slots or stackable slots in the chest
        ItemStack stack = new ItemStack(item, amount);
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isEmpty()) {
                inv.setStack(i, stack);
                return;
            } else if (inv.getStack(i).isOf(item) && inv.getStack(i).getCount() < item.getMaxCount()) {
                // Simple stacking logic
                int canAdd = item.getMaxCount() - inv.getStack(i).getCount();
                int adding = Math.min(canAdd, stack.getCount());
                inv.getStack(i).increment(adding);
                stack.decrement(adding);
                if (stack.isEmpty()) return;
            }
        }
    }
}
