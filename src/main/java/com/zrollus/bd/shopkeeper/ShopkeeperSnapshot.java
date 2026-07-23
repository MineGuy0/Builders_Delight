package com.zrollus.bd.shopkeeper;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ShopkeeperSnapshot {
    public final String name;
    public final long createdAt;
    final String shopName;
    final boolean closed;
    final boolean notifyOwner;
    final boolean exactItems;
    final int tradePermissionLevel;
    final List<ShopkeeperTrade> trades;

    private ShopkeeperSnapshot(String name, long createdAt, String shopName, boolean closed,
                               boolean notifyOwner, boolean exactItems, int tradePermissionLevel,
                               List<ShopkeeperTrade> trades) {
        this.name = name;
        this.createdAt = createdAt;
        this.shopName = shopName;
        this.closed = closed;
        this.notifyOwner = notifyOwner;
        this.exactItems = exactItems;
        this.tradePermissionLevel = tradePermissionLevel;
        this.trades = trades;
    }

    static ShopkeeperSnapshot capture(Shopkeeper shop, String name) {
        List<ShopkeeperTrade> trades = new ArrayList<>();
        for (ShopkeeperTrade trade : shop.trades)
            trades.add(new ShopkeeperTrade(trade.costOne, trade.costTwo, trade.result, trade.command));
        return new ShopkeeperSnapshot(name, Instant.now().toEpochMilli(), shop.name, shop.closed,
                shop.notifyOwner, shop.exactItems, shop.tradePermissionLevel, trades);
    }

    void apply(Shopkeeper shop) {
        shop.setName(Text.literal(shopName));
        shop.closed = closed;
        shop.notifyOwner = notifyOwner;
        shop.exactItems = exactItems;
        shop.tradePermissionLevel = tradePermissionLevel;
        shop.trades.clear();
        for (ShopkeeperTrade trade : trades)
            shop.trades.add(new ShopkeeperTrade(trade.costOne, trade.costTwo, trade.result, trade.command));
    }

    NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);
        nbt.putLong("created_at", createdAt);
        nbt.putString("shop_name", shopName);
        nbt.putBoolean("closed", closed);
        nbt.putBoolean("notify", notifyOwner);
        nbt.putBoolean("exact", exactItems);
        nbt.putInt("trade_permission_level", tradePermissionLevel);
        NbtList tradeList = new NbtList();
        for (ShopkeeperTrade trade : trades) tradeList.add(trade.toNbt(registries));
        nbt.put("trades", tradeList);
        return nbt;
    }

    static ShopkeeperSnapshot fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        List<ShopkeeperTrade> trades = new ArrayList<>();
        NbtList tradeList = nbt.getList("trades", 10);
        for (int i = 0; i < tradeList.size(); i++) trades.add(ShopkeeperTrade.fromNbt(tradeList.getCompound(i), registries));
        return new ShopkeeperSnapshot(nbt.getString("name"), nbt.getLong("created_at"), nbt.getString("shop_name"),
                nbt.getBoolean("closed"), nbt.getBoolean("notify"), !nbt.contains("exact") || nbt.getBoolean("exact"),
                nbt.getInt("trade_permission_level"), trades);
    }
}
