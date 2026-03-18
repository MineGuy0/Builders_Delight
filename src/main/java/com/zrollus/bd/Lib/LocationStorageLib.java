package com.zrollus.bd.Lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zrollus.bd.Lib.libHelpers.BDLocation;
import com.zrollus.bd.Lib.libHelpers.GlobalWarpModel;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocationStorageLib {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path getBdDataDir(MinecraftServer server) {
        // server.getSavePath is the only safe way to get the world folder in SP
        Path path = server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve("bd_data");
        try {
            if (!Files.exists(path)) Files.createDirectories(path);
            Path playerPath = path.resolve("players");
            if (!Files.exists(playerPath)) Files.createDirectories(playerPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }
    public static Map<String, BDLocation> getWaystoneWarps(MinecraftServer server) {
        Map<String, BDLocation> waystoneWarps = new HashMap<>();

        // Safety check: Don't even try if the server is stopping
        if (server == null) return waystoneWarps;

        Path path = server.getSavePath(WorldSavePath.GENERATED).resolve("waystones.dat");

        if (Files.exists(path)) {
            // Use a BufferedInputStream to read as fast as possible to minimize "hang" time
            try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
                NbtCompound nbt = NbtIo.readCompressed(is);
                if (nbt != null && nbt.contains("Waystones")) {
                    NbtList list = nbt.getList("Waystones", 10);
                    for (int i = 0; i < list.size(); i++) {
                        NbtCompound entry = list.getCompound(i);
                        if (entry.getBoolean("IsGlobal")) {
                            waystoneWarps.put(entry.getString("Name"), new BDLocation(
                                    entry.getInt("X") + 0.5,
                                    entry.getInt("Y") + 1.0,
                                    entry.getInt("Z") + 0.5,
                                    0, 0,
                                    entry.getString("World")
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                // If Kilt/Waystones has the file locked, we just return empty.
                // This prevents the "Server Thread Died" crash.
                System.err.println("[BD] Waystones.dat is currently busy. Skipping.");
            }
        }
        return waystoneWarps;
    }

    public static ServerWorld getWorldFromString(ServerPlayerEntity player, String worldId) {
        for (ServerWorld world : player.getServer().getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(worldId)) {
                return world;
            }
        }
        return player.getServer().getOverworld(); // Fallback
    }
    private static Path getDataDir(MinecraftServer server) {
        Path path = server.getSavePath(WorldSavePath.ROOT); // Standard world/data folder
        try {
            Files.createDirectories(path.resolve("data").resolve("bd_Data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static PlayerDataModel getPlayerData(MinecraftServer server, UUID uuid) {
        Path filePath = getBdDataDir(server).resolve("players").resolve(uuid.toString() + ".json");
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                return GSON.fromJson(reader, PlayerDataModel.class);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return new PlayerDataModel();
    }

    public static void savePlayerData(MinecraftServer server, UUID uuid, PlayerDataModel data) {
        Path filePath = getBdDataDir(server).resolve("players").resolve(uuid.toString() + ".json");
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            GSON.toJson(data, writer);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- Helper for /back ---
    public static void saveBackLocation(ServerPlayerEntity player) {
        PlayerDataModel data = getPlayerData(player.getServer(), player.getUuid());
        data.lastLocation = new BDLocation(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey().getValue().toString()
        );
        savePlayerData(player.getServer(), player.getUuid(), data);
    }

    // Updated to take 'server' as a parameter
    public static void saveGlobalWarps(MinecraftServer server, GlobalWarpModel model) {
        // We use getDataDir(server) here to match the getter!
        Path filePath = getDataDir(server).resolve("data").resolve("bd_Data").resolve("bd_warps.json");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            GSON.toJson(model, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GlobalWarpModel getGlobalWarps(MinecraftServer server) {
        Path filePath = getDataDir(server).resolve("data").resolve("bd_Data").resolve("bd_warps.json");
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                GlobalWarpModel model = GSON.fromJson(reader, GlobalWarpModel.class);
                return model != null ? model : new GlobalWarpModel();
            } catch (IOException e) { e.printStackTrace(); }
        }
        return new GlobalWarpModel();
    }
}