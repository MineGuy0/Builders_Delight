package com.zrollus.bd.Lib;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.registry.RegistryWrapper;
import java.util.HashMap;
import java.util.Map;

public class DynamicIdMapper extends PersistentState {
    private final Map<Integer, String> idToName = new HashMap<>();
    private final Map<String, Integer> nameToId = new HashMap<>();
    private int nextId = 1;

    // --- LOGIC ---

    public void generateAllIds() {
        int newItems = 0;
        // Loop through every item registered (Minecraft + all Mods)
        for (Identifier id : Registries.ITEM.getIds()) {
            String fullName = id.toString();

            // If the item isn't in our .dat file yet, add it!
            if (!nameToId.containsKey(fullName)) {
                getOrCreateIndex(fullName);
                newItems++;
            }
        }
        if (newItems > 0) {
            System.out.println("BD-Economy: Auto-indexed " + newItems + " new items into .dat file.");
        }
    }

    public String getName(int id) {
        return idToName.getOrDefault(id, null);
    }

    public int getOrCreateIndex(String itemName) {
        if (nameToId.containsKey(itemName)) {
            return nameToId.get(itemName);
        }

        int id = nextId++;
        idToName.put(id, itemName);
        nameToId.put(itemName, id);
        this.markDirty(); // Tells Minecraft to save the .dat file
        return id;
    }

    // --- BOILERPLATE / SAVING ---

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        idToName.forEach((id, name) -> {
            NbtCompound entry = new NbtCompound();
            entry.putInt("i", id);
            entry.putString("n", name);
            list.add(entry);
        });
        nbt.put("mappings", list);
        nbt.putInt("next", nextId);
        return nbt;
    }

    public static DynamicIdMapper readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        DynamicIdMapper state = new DynamicIdMapper();
        NbtList list = nbt.getList("mappings", 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            int id = entry.getInt("i");
            String name = entry.getString("n");
            state.idToName.put(id, name);
            state.nameToId.put(name, id);
        }
        state.nextId = nbt.getInt("next");
        return state;
    }

    // --- ACCESSOR ---
    public static DynamicIdMapper getServerState(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(DynamicIdMapper::new, DynamicIdMapper::readNbt, null),
                "bd_id_mappings");
    }
}
