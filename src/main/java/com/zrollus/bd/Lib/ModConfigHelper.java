package com.zrollus.bd.Lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfigHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("bd-mod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("BuildersDelight_Config.json");

    // This is the instance we keep in memory
    private static ModConfigHelper instance;

    // --- CONFIG VALUES START HERE ---
    public int tpaRequestTimeout = 120;
    public int homeLimit = 6;
    public int autoAfkSeconds = 300;
    public boolean cancelAfkOnMove = true;
    public int teleportCooldownSeconds = 0;
    public int teleportWarmupSeconds = 0;
    public int teleportInvulnerabilitySeconds = 4;
    public String motd = "&6&lWelcome to the server!";
    public String rules = "&eBe respectful, do not grief, and have fun.";
    public String serverInfo = "&6Builder's Delight &7server utilities are enabled.";
    public String nicknamePrefix = "~";
    public int maxVaults = 8;
    public int maxShopkeepersPerPlayer = 10;
    public int maxShopMembers = 9;
    public int maxShopTradePages = 5;
    public boolean protectShopContainers = true;
    public boolean preventTradingWithOwnShop = true;
    public boolean defaultExactShopItems = true;
    public boolean defaultShopTradeNotifications = true;
    // You can easily add more here later, like:
    // public boolean playSoundOnTeleport = true;
    // --- CONFIG VALUES END HERE ---

    /**
     * Call this in your ModInitializer's onInitialize()
     */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, ModConfigHelper.class);
                LOGGER.info("BD Mod config loaded successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults.", e);
                instance = new ModConfigHelper();
            }
            save(); // Persist any newly added settings with their defaults after closing the reader.
        } else {
            instance = new ModConfigHelper();
            save(); // Create the file if it doesn't exist
        }
    }

    /**
     * Saves the current in-memory settings to the disk
     */
    public static void save() {
        if (instance == null) instance = new ModConfigHelper();
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config.", e);
        }
    }

    /**
     * Use this to get values: ModConfigHelper.get().timeoutSeconds
     */
    public static ModConfigHelper get() {
        if (instance == null) load();
        return instance;
    }
}
