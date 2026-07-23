package com.zrollus.bd.shopkeeper;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShopkeeperManager extends PersistentState {
    private final Map<UUID, Shopkeeper> shops = new LinkedHashMap<>();
    private final List<String> history = new ArrayList<>();

    public Collection<Shopkeeper> all() { return shops.values(); }
    public Shopkeeper get(UUID id) { return shops.get(id); }

    public Shopkeeper byEntity(UUID entityId) {
        for (Shopkeeper shop : shops.values()) if (entityId.equals(shop.entityId)) return shop;
        return null;
    }

    public Shopkeeper bySign(String worldId, BlockPos pos) {
        for (Shopkeeper shop : shops.values()) {
            if (shop.signObject && shop.worldId.equals(worldId) && shop.objectPos.equals(pos)) return shop;
        }
        return null;
    }

    public Shopkeeper byContainer(String worldId, BlockPos pos) {
        for (Shopkeeper shop : shops.values()) {
            if (shop.containerPos != null && shop.containerWorldId.equals(worldId) && shop.containerPos.equals(pos)) return shop;
        }
        return null;
    }

    public void add(Shopkeeper shop) { shops.put(shop.id, shop); markDirty(); }
    public Shopkeeper remove(UUID id) { Shopkeeper removed = shops.remove(id); if (removed != null) markDirty(); return removed; }
    public void changed() { markDirty(); }
    public void log(String line) { history.add(0, line); while (history.size() > 500) history.remove(history.size() - 1); markDirty(); }
    public List<String> history() { return List.copyOf(history); }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (Shopkeeper shop : shops.values()) list.add(shop.toNbt(registries));
        nbt.put("shops", list);
        NbtList historyList = new NbtList();
        for (String line : history) {
            NbtCompound entry = new NbtCompound();
            entry.putString("line", line);
            historyList.add(entry);
        }
        nbt.put("history", historyList);
        return nbt;
    }

    private static ShopkeeperManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ShopkeeperManager manager = new ShopkeeperManager();
        NbtList list = nbt.getList("shops", 10);
        for (int i = 0; i < list.size(); i++) {
            Shopkeeper shop = Shopkeeper.fromNbt(list.getCompound(i), registries);
            manager.shops.put(shop.id, shop);
        }
        NbtList historyList = nbt.getList("history", 10);
        for (int i = 0; i < historyList.size(); i++) manager.history.add(historyList.getCompound(i).getString("line"));
        return manager;
    }

    public static ShopkeeperManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(ShopkeeperManager::new, ShopkeeperManager::fromNbt, null),
                "bd_shopkeepers");
    }
}
