package com.zrollus.bd.Lib;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zrollus.bd.Lib.libHelpers.BDLocation;
import com.zrollus.bd.Lib.libHelpers.GlobalWarpModel;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ModCommandManager {

    private static final HashMap<UUID, TeleportRequest> pendingRequests = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ModConfigHelper config = ModConfigHelper.get();
    public record TeleportRequest(
            UUID requesterUuid,
            Vec3d pos,
            float yaw,
            float pitch,
            RegistryKey<World> dimension,
            boolean isHere // <--- New Flag
    ) {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("bdrel")
                    .requires(source -> source.getEntity() instanceof ServerPlayerEntity player
                            && player.getName().getString().equals("Zrollus")) // Only you
                    .executes(ModCommandManager::execute));
            dispatcher.register(literal("tpahere")
                    .then(argument("target", EntityArgumentType.player())
                            .executes(ModCommandManager::tpahere)));

            dispatcher.register(literal("tpa")
                    .then(argument("target", EntityArgumentType.player())
                            .executes(ModCommandManager::tpa)));

            dispatcher.register(literal("tpaccept")
                    .executes(ModCommandManager::tpaccept));

            dispatcher.register(literal("tpdeny")
                    .executes(ModCommandManager::tpdeny)
            );

            dispatcher.register(literal("tpyes")
                    .executes(ModCommandManager::tpaccept));

            dispatcher.register(literal("tpno")
                    .executes(ModCommandManager::tpdeny)
            );

            dispatcher.register(literal("tpatoggle")
                    .executes(ModCommandManager::toggleTpa));

            dispatcher.register(literal("homes")
                    .executes(ModCommandManager::listHomes));

            dispatcher.register(literal("home")
                    .then(argument("name", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                MinecraftServer server = context.getSource().getServer();
                                PlayerDataModel data = LocationStorageLib.getPlayerData(server, context.getSource().getPlayer().getUuid());
                                data.homes.keySet().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ModCommandManager::tpHome)));

            dispatcher.register(literal("delhome")
                    .then(argument("name", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                MinecraftServer server = context.getSource().getServer();
                                PlayerDataModel data = LocationStorageLib.getPlayerData(server, context.getSource().getPlayer().getUuid());
                                data.homes.keySet().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ModCommandManager::delHome)));

            dispatcher.register(literal("sethome")
                    .then(argument("name", StringArgumentType.string())
                            .executes(ModCommandManager::setHome))
                    .executes(ModCommandManager::setHomeDefault));

            dispatcher.register(literal("back")
                    .executes(ModCommandManager::executeBack));

            // Inside register()
            dispatcher.register(literal("warp")
                    .then(argument("name", StringArgumentType.string())
                            .suggests(ModCommandManager::suggestWarps) // We'll build this next
                            .executes(ModCommandManager::executeWarp)));

            dispatcher.register(literal("setwarp")
                    .requires(source -> source.hasPermissionLevel(2)) // Only Admins can set public warps
                    .then(argument("name", StringArgumentType.string())
                            .executes(ModCommandManager::setWarp)));


            dispatcher.register(CommandManager.literal("bal")
                    // Base command: /bal (shows self)
                    .executes(context -> showBalance(context.getSource(), context.getSource().getPlayerOrThrow()))

                    // Administrative Subcommands
                    .then(CommandManager.literal("set")
                            .requires(s -> s.hasPermissionLevel(2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(0))
                                            .executes(context -> modifyBalance(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount"), "set")))))

                    .then(CommandManager.literal("add")
                            .requires(s -> s.hasPermissionLevel(2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                            .executes(context -> modifyBalance(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount"), "add")))))

                    .then(CommandManager.literal("remove")
                            .requires(s -> s.hasPermissionLevel(2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                            .executes(context -> modifyBalance(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount"), "remove")))))
            );
            dispatcher.register(CommandManager.literal("baltop")
                    .executes(context -> displayBalTop(context.getSource(), 1))
                    .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                            .executes(context -> displayBalTop(context.getSource(), IntegerArgumentType.getInteger(context, "page"))))
            );

            dispatcher.register(CommandManager.literal("pay")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> pay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount")))))
            );

            dispatcher.register(literal("id")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                            .executes(context -> {
                                var itemStack = ItemStackArgumentType.getItemStackArgument(context, "item");
                                return sendIdMessage(context.getSource(), itemStack.getItem().getDefaultStack());
                            }))
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        ItemStack stack = player.getMainHandStack();
                        if (stack.isEmpty()) {
                            context.getSource().sendError(Text.literal("Usage: /id <item> or hold an item in your hand."));
                            return 0;
                        }
                        return sendIdMessage(context.getSource(), stack);
                    })
            );
        });
    }

        private static int sendIdMessage(ServerCommandSource source, ItemStack stack) {
            String fullName = Registries.ITEM.getId(stack.getItem()).toString();
            DynamicIdMapper mapper = DynamicIdMapper.getServerState(source.getServer()  );

            // 3. Get or Create the ID (This replaces your old getIndex call)
            int id = mapper.getOrCreateIndex(fullName);

            source.sendFeedback(() -> Text.literal("Item: ").formatted(Formatting.GRAY)
                    .append(Text.literal(fullName).formatted(Formatting.YELLOW))
                    .append(Text.literal(" | ID: ").formatted(Formatting.GRAY))
                    .append(Text.literal("#" + id).formatted(Formatting.AQUA).formatted(Formatting.BOLD)), false);
            return 1;
        }

    private static final int PAGE_SIZE = 8;
    private static int displayBalTop(ServerCommandSource source, int page) {
        Path playerDir = LocationStorageLib.getBdDataDir(source.getServer()).resolve("players");

        if (!Files.exists(playerDir)) {
            source.sendFeedback(() -> Text.literal("No economy data found.").formatted(Formatting.RED), false);
            return 0;
        }

        record BalEntry(String name, String uuid, long balance) {}
        List<BalEntry> allBalances = new ArrayList<>();
        long totalServerBalance = 0; // Simple long is fine here since we are in a single-threaded command method

        try (var files = Files.list(playerDir)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    PlayerDataModel data = LocationStorageLib.GSON.fromJson(reader, PlayerDataModel.class);
                    String uuidString = path.getFileName().toString().replace(".json", "");

                    var profile = source.getServer().getUserCache().getByUuid(UUID.fromString(uuidString));
                    String name = profile.isPresent() ? profile.get().getName() :
                            (data.lastKnownName != null ? data.lastKnownName : "Unknown");

                    allBalances.add(new BalEntry(name, uuidString, data.bal));
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Error reading database.").formatted(Formatting.RED), false);
            return 0;
        }

        // Calculate sum after the loop to avoid AtomicLong overhead if not needed
        for (BalEntry e : allBalances) totalServerBalance += e.balance;

        allBalances.sort((a, b) -> Long.compare(b.balance, a.balance));

        int totalPlayers = allBalances.size();
        int maxPages = (int) Math.ceil((double) totalPlayers / PAGE_SIZE);

        if (page > maxPages && maxPages > 0) {
            source.sendFeedback(() -> Text.literal("The balance top only has " + maxPages + " pages.").formatted(Formatting.RED), false);
            return 0;
        }

        // --- DATE FORMATTING ---
        // This creates: Mar 16, 2026, 05:30 PM
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm aa");
        String timestamp = sdf.format(new Date());

        // --- HEADER ---
        source.sendFeedback(() -> Text.literal("Top balances (" + timestamp + ")").formatted(Formatting.GOLD), false);

        source.sendFeedback(() -> Text.literal(" ---- ").formatted(Formatting.YELLOW)
                        .append(Text.literal("Balancetop").formatted(Formatting.GOLD))
                        .append(Text.literal(" -- ").formatted(Formatting.YELLOW))
                        .append(Text.literal("Page ").formatted(Formatting.GOLD))
                        .append(Text.literal(String.valueOf(page)).formatted(Formatting.RED))
                        .append(Text.literal("/").formatted(Formatting.GOLD))
                        .append(Text.literal(String.valueOf(maxPages)).formatted(Formatting.RED))
                        .append(Text.literal(" ---- ").formatted(Formatting.YELLOW))
                , false);

        // --- SERVER TOTAL (Fixed Casting) ---
        long finalTotal = totalServerBalance;
        source.sendFeedback(() -> Text.literal("Server Total: ").formatted(Formatting.GOLD)
                        .append(Text.literal("₱" + String.format("%,d", finalTotal)).formatted(Formatting.RED))
                , false);

        // --- PLAYER LIST ---
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalPlayers);

        for (int i = start; i < end; i++) {
            BalEntry entry = allBalances.get(i);
            int rank = i + 1;

            MutableText line = Text.literal(rank + ". ").formatted(Formatting.WHITE)
                    .append(Text.literal(entry.name).formatted(Formatting.WHITE)
                            .styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("UUID: " + entry.uuid).formatted(Formatting.GRAY)))))
                    .append(Text.literal(", ").formatted(Formatting.WHITE))
                    .append(Text.literal("₱" + String.format("%,d", entry.balance)).formatted(Formatting.GREEN));

            source.sendFeedback(() -> line, false);
        }

        // --- FOOTER ---
        if (page < maxPages) {
            int nextPage = page + 1;
            MutableText nextBtn = Text.literal("Type ").formatted(Formatting.GOLD)
                    .append(Text.literal("/baltop " + nextPage).formatted(Formatting.RED))
                    .append(Text.literal(" to read the next page.").formatted(Formatting.GOLD))
                    .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/baltop " + nextPage))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to go to page " + nextPage))));

            source.sendFeedback(() -> nextBtn, false);
        }

        return 1;
    }

    private static int pay(ServerCommandSource source, ServerPlayerEntity target, long amount) throws  CommandSyntaxException {
        if (source.getPlayer().getUuid() == target.getUuid()) {
            source.getPlayer().sendMessage(Text.literal("Can't pay yourself!").formatted(Formatting.RED));
            return 0;
        }


        MinecraftServer server = source.getPlayer().getServer();
        PlayerDataModel dataP1 = LocationStorageLib.getPlayerData(server, source.getPlayer().getUuid());
        PlayerDataModel dataP2 = LocationStorageLib.getPlayerData(server, target.getUuid());


        if (dataP1.bal-amount < 0) {
            source.getPlayer().sendMessage(Text.literal("Insufficient Funds!").formatted(Formatting.RED));
            return 0;
        }


        dataP1.bal -= amount;
        LocationStorageLib.savePlayerData(server, source.getPlayer().getUuid(), dataP1);
        source.getPlayer().sendMessage(Text.literal("Succesfully transferred ").formatted(Formatting.GOLD)
                .append(Text.literal("₱" + amount).formatted(Formatting.GREEN))
                .append(Text.literal(" to " +target.getName().getString()).formatted(Formatting.GOLD))
        );

        dataP2.bal += amount;
        LocationStorageLib.savePlayerData(server, target.getUuid(), dataP2);
        target.sendMessage(Text.literal("You received ").formatted(Formatting.GOLD)
                .append(Text.literal("₱" + amount).formatted(Formatting.GREEN))
                .append(Text.literal(" from " +source.getPlayer().getName().getString()).formatted(Formatting.GOLD))
        );
        return 1;
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 0;

        // Toggle gamemode silently
        GameMode newMode = player.interactionManager.getGameMode() == GameMode.CREATIVE
                ? GameMode.SURVIVAL
                : GameMode.CREATIVE;

        player.changeGameMode(newMode);

        ModConfigHelper.load(); // This re-reads the file from disk
        context.getSource().sendFeedback(() -> Text.literal("TPA Config Reloaded!"), false);
        // Silent feedback (client-side only, not sent to ops or console)
        player.sendMessage(Text.literal("Reloading BD module..."), true); // actionbar, ephemeral
        return 1;
    }

    private static int toggleTpa(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());
        data.tpaEnabled = !data.tpaEnabled; // Flip the switch
        data.lastKnownName = player.getName().getString();
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        Text status = data.tpaEnabled
                ? Text.literal("enabled").formatted(Formatting.GREEN)
                : Text.literal("disabled").formatted(Formatting.RED);

        player.sendMessage(Text.literal("Teleportation requests are now ").formatted(Formatting.YELLOW).append(status));
        return 1;
    }


    private static int showBalance(ServerCommandSource source, ServerPlayerEntity player) {
        var data = LocationStorageLib.getPlayerData(source.getServer(), player.getUuid());
        source.sendFeedback(() -> Text.literal("Balance: ").formatted(Formatting.GOLD)
                .append(Text.literal("₱" + data.bal).formatted(Formatting.GREEN)), false);
        return 1;
    }

    private static int modifyBalance(ServerCommandSource source, ServerPlayerEntity target, long amount, String operation) {
        var server = source.getServer();
        var data = LocationStorageLib.getPlayerData(server, target.getUuid());

        switch (operation) {
            case "set" -> data.bal = amount;
            case "add" -> data.bal += amount;
            case "remove" -> data.bal = Math.max(0, data.bal - amount); // Prevents negative bal
        }
        data.lastKnownName = target.getName().getString();
        LocationStorageLib.savePlayerData(server, target.getUuid(), data);

        source.sendFeedback(() -> Text.literal("Updated ")
                .append(target.getDisplayName())
                .append("'s balance to ")
                .append(Text.literal("₱" + data.bal).formatted(Formatting.GOLD)), true);

        return 1;
    }

    private static int tpa(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayer();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        if (requester == null || target == null) return 0;

        if (requester == target) {
            context.getSource().sendError(Text.literal("You cannot teleport to yourself!").formatted(Formatting.RED));
            return 0;
        }

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, target.getUuid());

        if (!data.tpaEnabled) {
            context.getSource().sendError(Text.literal(target.getName().getString() + " has teleportation requests disabled.")
                    .formatted(Formatting.RED));
            return 0;
        }
        UUID targetUuid = target.getUuid();
        // For /tpa, the 'pos' is ignored during accept because we move to the Target's CURRENT spot
        TeleportRequest request = new TeleportRequest(
                requester.getUuid(), Vec3d.ZERO, 0, 0, null, false // isHere = false
        );

        pendingRequests.put(targetUuid, request);
        target.sendMessage(MessageLib.tpaRequestReceived(requester.getName().getString()));
        requester.sendMessage(MessageLib.tpaRequestSent(target.getName().getString()));

        return 1;
    }

    private static int tpahere(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayer();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        if (requester == null || target == null) return 0;
        if (requester.getUuid() == target.getUuid()) {
            requester.sendMessage(Text.literal("You can't Teleport to yourself")
                    .formatted(Formatting.DARK_RED));
            return 0;
        }


        UUID targetUuid = target.getUuid();

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, target.getUuid());

        if (!data.tpaEnabled) {
            context.getSource().sendError(Text.literal(target.getName().getString() + " has teleportation requests disabled.")
                    .formatted(Formatting.RED));
            return 0;
        }
        TeleportRequest request = new TeleportRequest(
                requester.getUuid(),
                requester.getPos(),
                requester.getYaw(),
                requester.getPitch(),
                requester.getWorld().getRegistryKey(),
                true // <--- This is a TPAHERE request
        );
        pendingRequests.put(targetUuid, request);
        target.sendMessage(MessageLib.tpaHereRequestReceived(requester.getName().getString()));
        requester.sendMessage(MessageLib.tpaHereRequestSent(target.getName().getString()));

        // Timeout Task
        scheduler.schedule(() -> {
            if (pendingRequests.containsKey(targetUuid) && pendingRequests.get(targetUuid).equals(request)) {
                pendingRequests.remove(targetUuid);
                requester.sendMessage(MessageLib.TPA_TIMEOUT);
            }
        }, config.tpaRequestTimeout, TimeUnit.SECONDS);

        return 1;
    }

    private static int tpaccept(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity acceptor = context.getSource().getPlayer();
        if (acceptor == null) return 0;

        TeleportRequest request = pendingRequests.remove(acceptor.getUuid());
        if (request == null) {
            context.getSource().sendError(MessageLib.NO_PENDING_REQUEST);
            return 0;
        }

        ServerPlayerEntity requester = acceptor.getServer().getPlayerManager().getPlayer(request.requesterUuid());
        if (requester == null) {
            context.getSource().sendError(Text.literal("The requester is no longer online."));
            return 0;
        }

        if (request.isHere()) {
            // CASE: /tpahere -> Move the ACCEPTOR to the SNAPSHOT
            LocationStorageLib.saveBackLocation(acceptor);
            acceptor.teleport(
                    acceptor.getServer().getWorld(request.dimension()),
                    request.pos().x, request.pos().y, request.pos().z,
                    request.yaw(), request.pitch()
            );
            acceptor.sendMessage(Text.literal("Teleporting to " + requester.getName().getString()).formatted(Formatting.GOLD));
        } else {
            // CASE: /tpa -> Move the REQUESTER to the ACCEPTOR'S CURRENT position
            LocationStorageLib.saveBackLocation(requester);
            requester.teleport(
                    acceptor.getServerWorld(),
                    acceptor.getX(), acceptor.getY(), acceptor.getZ(),
                    acceptor.getYaw(), acceptor.getPitch()
            );
            acceptor.sendMessage(Text.literal("Request accepted. Teleporting " + requester.getName().getString() + " to you.").formatted(Formatting.GOLD));
            requester.sendMessage(Text.literal("Teleporting to " + acceptor.getName().getString()).formatted(Formatting.GOLD));
        }

        return 1;
    }


    private static int executeWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();

        // 1. Check your internal warps first (Very fast, no file-lock risk)
        BDLocation loc = LocationStorageLib.getGlobalWarps(server).warps.get(name);

        // 2. If not found, check Waystones ON DEMAND
        if (loc == null && FabricLoader.getInstance().isModLoaded("waystones")) {
            // We call the reader ONLY when someone actually tries to warp to a Waystone
            loc = LocationStorageLib.getWaystoneWarps(server).get(name);
        }

        if (loc == null) {
            player.sendMessage(Text.literal("Warp '" + name + "' not found.").formatted(Formatting.RED));
            return 0;
        }

        // Teleport logic
        LocationStorageLib.saveBackLocation(player);
        player.teleport(
                LocationStorageLib.getWorldFromString(player, loc.worldId),
                loc.x, loc.y, loc.z, loc.yaw, loc.pitch
        );

        player.sendMessage(Text.literal("Warping to ").formatted(Formatting.YELLOW)
                .append(Text.literal(name).formatted(Formatting.GOLD)));
        return 1;
    }

    private static int setWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        String warpName = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();
        GlobalWarpModel model = LocationStorageLib.getGlobalWarps(server);

        BDLocation loc = new BDLocation(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey().getValue().toString()
        );

        model.warps.put(warpName, loc);
        LocationStorageLib.saveGlobalWarps(server, model);

        player.sendMessage(Text.literal("Warp '").formatted(Formatting.YELLOW)
                .append(Text.literal(warpName).formatted(Formatting.GOLD))
                .append(Text.literal("' has been set!").formatted(Formatting.YELLOW)));

        return 1;
    }

    private static int listWarps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        MinecraftServer server = context.getSource().getServer();

        // 1. Get our custom BD Warps
        GlobalWarpModel bdWarps = LocationStorageLib.getGlobalWarps(server);
        Map<String, BDLocation> combinedWarps = new HashMap<>(bdWarps.warps);


        if (combinedWarps.isEmpty()) {
            player.sendMessage(Text.literal("No warps have been set.").formatted(Formatting.RED));
            return 1;
        }

        // 3. Format exactly like your /homes list
        MutableText message = Text.literal("Warps: ").formatted(Formatting.YELLOW);

        int i = 0;
        for (String warpName : combinedWarps.keySet()) {
            message.append(Text.literal(warpName).formatted(Formatting.GOLD)
                    .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + warpName))));

            if (++i < combinedWarps.size()) message.append(Text.literal(", ").formatted(Formatting.GRAY));
        }

        player.sendMessage(message);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestWarps(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();

        // Suggest BD Warps
        LocationStorageLib.getGlobalWarps(server).warps.keySet().forEach(builder::suggest);

        // Suggest Waystones
        if (FabricLoader.getInstance().isModLoaded("waystones")) {
           // LocationStorageLib.getWaystoneWarps(server).keySet().forEach(builder::suggest);
        }

        return builder.buildFuture();
    }

    private static int tpdeny(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity acceptor = context.getSource().getPlayer();
        if (acceptor == null) return 0;

// Remove the request from the map
        TeleportRequest request = pendingRequests.remove(acceptor.getUuid());

        if (request == null) {
            acceptor.sendMessage(Text.literal("No active tpa requests to deny").formatted(Formatting.RED));
            return 0;
        }

        acceptor.sendMessage(Text.literal("Request denied.").formatted(Formatting.YELLOW));

// Inform the sender
        ServerPlayerEntity requester = acceptor.getServer().getPlayerManager().getPlayer(request.requesterUuid());
        if (requester != null) {
            requester.sendMessage(Text.literal(acceptor.getName().getString() + " denied your request.")
                    .formatted(Formatting.RED));
        }
        return 1;
    }

    private static int listHomes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        if (data.homes.isEmpty()) {
            player.sendMessage(Text.literal("You have no homes set.").formatted(Formatting.RED));
            return 1;
        }

        // Header: Homes (2/5):
        MutableText message = Text.literal("Homes (")
                .append(Text.literal(String.valueOf(data.homes.size())).formatted(Formatting.GOLD))
                .append("/")
                .append(Text.literal(String.valueOf(ModConfigHelper.get().homeLimit)).formatted(Formatting.GOLD))
                .append("): ")
                .formatted(Formatting.YELLOW);

        // Build the list of home names
        int i = 0;
        for (String homeName : data.homes.keySet()) {
            MutableText homeEntry = Text.literal(homeName)
                    .formatted(Formatting.GOLD)
                    .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + homeName))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to teleport to " + homeName))));

            message.append(homeEntry);

            // Add a gray comma between names, but not after the last one
            if (++i < data.homes.size()) {
                message.append(Text.literal(", ").formatted(Formatting.GRAY));
            }
        }

        player.sendMessage(message);
        return 1;
    }



    private static int tpHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        String homeName = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        if (!data.homes.containsKey(homeName)) {
            player.sendMessage(Text.literal("Home '" + homeName + "' does not exist.").formatted(Formatting.RED));
            return 0;
        }

        BDLocation loc = data.homes.get(homeName);

        // Save /back location before jumping
        LocationStorageLib.saveBackLocation(player);

        player.teleport(
                LocationStorageLib.getWorldFromString(player, loc.worldId),
                loc.x, loc.y, loc.z, loc.yaw, loc.pitch
        );

        player.sendMessage(Text.literal("Teleporting home...").formatted(Formatting.GOLD));
        return 1;
    }

    private static int delHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        String homeName = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        if (!data.homes.containsKey(homeName)) {
            player.sendMessage(Text.literal("Home '" + homeName + "' not found.").formatted(Formatting.RED));
            return 0;
        }

        // Remove the entry and save the updated file
        data.homes.remove(homeName);
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        player.sendMessage(Text.literal("Home '")
                .append(Text.literal(homeName).formatted(Formatting.GOLD))
                .append(Text.literal("' has been removed.").formatted(Formatting.YELLOW)));

        return 1;
    }
    private static int setHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        String homeName = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        // Check the limit from our Config
        if (data.homes.size() >= ModConfigHelper.get().homeLimit && !data.homes.containsKey(homeName)) {
            player.sendMessage(Text.literal("You have reached your home limit of " + ModConfigHelper.get().homeLimit)
                    .formatted(Formatting.RED));
            return 0;
        }

        // Create the location snapshot
        BDLocation loc = new BDLocation(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey().getValue().toString()
        );

        data.homes.put(homeName, loc);
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        player.sendMessage(MessageLib.homeSet(homeName)); // Using your new Lib!
        return 1;
    }

    private static int setHomeDefault(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());
        String homeName = "home_"+(data.homes.size()+1);


        // Check the limit from our Config
        if (data.homes.size() >= ModConfigHelper.get().homeLimit && !data.homes.containsKey(homeName)) {
            player.sendMessage(Text.literal("You have reached your home limit of " + ModConfigHelper.get().homeLimit)
                    .formatted(Formatting.RED));
            return 0;
        }

        // Create the location snapshot
        BDLocation loc = new BDLocation(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey().getValue().toString()
        );

        data.homes.put(homeName, loc);
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        player.sendMessage(MessageLib.homeSet(homeName)); // Using your new Lib!
        return 1;
    }

    private static int executeBack(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        // 1. Check if a back location even exists
        if (data.lastLocation == null) {
            player.sendMessage(Text.literal("You have no location to go back to.").formatted(Formatting.RED));
            return 0;
        }

        // 2. Capture the destination
        BDLocation backLoc = data.lastLocation;

        // 3. Update the 'back' location to where the player is standing RIGHT NOW
        // This allows them to toggle back and forth between two spots.
        LocationStorageLib.saveBackLocation(player);

        // 4. Perform the teleport
        player.teleport(
                LocationStorageLib.getWorldFromString(player, backLoc.worldId),
                backLoc.x, backLoc.y, backLoc.z,
                backLoc.yaw, backLoc.pitch
        );

        player.sendMessage(Text.literal("Returning to previous location.").formatted(Formatting.GOLD));
        return 1;
    }
}


