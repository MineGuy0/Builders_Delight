package com.zrollus.bd.Lib;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class PokedollarHandler {
    // Replace with your actual Pokedollar Item instance
    public static final Item POKEDOLLAR_ITEM = Registries.ITEM.get(Identifier.of("bd", "pokedollar"));

    public static void syncPhysicalToVirtual(ServerPlayerEntity player) {
        long physicalCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(POKEDOLLAR_ITEM)) {
                physicalCount += stack.getCount();
                stack.setCount(0); // Remove from inventory
            }
        }

        if (physicalCount > 0) {
            var data = LocationStorageLib.getPlayerData(player.getServer(), player.getUuid());
            data.bal += physicalCount;
            LocationStorageLib.savePlayerData(player.getServer(), player.getUuid(), data);
            player.sendMessage(Text.literal("Deposited ₱" + physicalCount + " from inventory.").formatted(Formatting.GRAY), true);
        }
    }
}
