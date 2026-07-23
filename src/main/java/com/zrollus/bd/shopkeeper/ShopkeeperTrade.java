package com.zrollus.bd.shopkeeper;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

public final class ShopkeeperTrade {
    public ItemStack costOne = ItemStack.EMPTY;
    public ItemStack costTwo = ItemStack.EMPTY;
    public ItemStack result = ItemStack.EMPTY;
    public String command = "";

    public ShopkeeperTrade() {}

    public ShopkeeperTrade(ItemStack costOne, ItemStack costTwo, ItemStack result) {
        this(costOne, costTwo, result, "");
    }

    public ShopkeeperTrade(ItemStack costOne, ItemStack costTwo, ItemStack result, String command) {
        this.costOne = costOne.copy();
        this.costTwo = costTwo.copy();
        this.result = result.copy();
        this.command = command == null ? "" : command;
    }

    /** Cost two cannot stand alone in the vanilla merchant format. Promote it when needed. */
    public boolean normalizeCosts() {
        if (!costOne.isEmpty() || costTwo.isEmpty()) return false;
        costOne = costTwo;
        costTwo = ItemStack.EMPTY;
        return true;
    }

    NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        nbt.put("cost_one", costOne.encodeAllowEmpty(registries));
        nbt.put("cost_two", costTwo.encodeAllowEmpty(registries));
        nbt.put("result", result.encodeAllowEmpty(registries));
        if (!command.isBlank()) nbt.putString("command", command);
        return nbt;
    }

    static ShopkeeperTrade fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ShopkeeperTrade trade = new ShopkeeperTrade();
        trade.costOne = ItemStack.fromNbt(registries, nbt.get("cost_one")).orElse(ItemStack.EMPTY);
        trade.costTwo = ItemStack.fromNbt(registries, nbt.get("cost_two")).orElse(ItemStack.EMPTY);
        trade.result = ItemStack.fromNbt(registries, nbt.get("result")).orElse(ItemStack.EMPTY);
        trade.command = nbt.getString("command");
        return trade;
    }
}
