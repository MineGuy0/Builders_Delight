package com.zrollus.bd.Lib;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.zrollus.bd.EventHandler.LockEventHandler;
import com.zrollus.bd.GUI.InvSeeScreenHandler;
import com.zrollus.bd.GUI.PlayerVaultScreenHandler;
import com.zrollus.bd.GUI.VaultUtils;
import com.zrollus.bd.Lib.libHelpers.AdminStateRegistry;
import com.zrollus.bd.Lib.libHelpers.BDLocation;
import com.zrollus.bd.Lib.libHelpers.GlobalWarpModel;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
            dispatcher.register(literal("emsg")
                .then(argument("target", EntityArgumentType.player())
                    .then(argument("msg", MessageArgumentType.message())
                            .executes(ModCommandManager::msg)
                    )
                )
            );

            dispatcher.register(literal("r")
                .then(argument("msg", MessageArgumentType.message())
                        .executes(ModCommandManager::respond)
                )
            );

            dispatcher.register(literal("mail")
                .then(literal("read").executes(ModCommandManager::readMail))
                .then(literal("clear").executes(ModCommandManager::clearMail))
                .then(literal("send")
                    .then(argument("targetName", StringArgumentType.string())
                        .then(argument("msg", MessageArgumentType.message())
                            .executes(ModCommandManager::sendMail)))));

            dispatcher.register(literal("whereami")
                .executes(ModCommandManager::whereami)
            );

            dispatcher.register(literal("tpo")
                    .then(argument("targetName", StringArgumentType.string())
                            .executes(ModCommandManager::tpo)));

            dispatcher.register(literal("tpohere")
                    .then(argument("targetName", StringArgumentType.string())
                            .executes(ModCommandManager::tpohere)));

            dispatcher.register(literal("seen")
                    .then(argument("targetName", StringArgumentType.string())
                            .executes(ModCommandManager::seen)));

            dispatcher.register(literal("nick")
                            .then(argument("player", EntityArgumentType.player())
                                    .then(argument("nick", StringArgumentType.greedyString())
                                            .executes(ModCommandManager::setNickname)
                            )));

            dispatcher.register(literal("nickname")
                    .then(argument("player", EntityArgumentType.player())
                            .then(argument("nick", StringArgumentType.greedyString())
                                    .executes(ModCommandManager::setNickname)
                            )));
            int vaultLimit = ModConfigHelper.get().maxVaults;

            dispatcher.register(literal("pv")
                    // 1. Path for self: /pv <number>
                    .then(argument("vaultNumber", IntegerArgumentType.integer(1, vaultLimit))
                            .executes(ModCommandManager::openPVault))

                    // 2. Path for admins: /pv open <player> <number>
                    .then(literal("open")
                            .requires(s -> s.hasPermissionLevel(2))
                            .then(argument("player", StringArgumentType.string())
                                    .then(argument("vaultNumber", IntegerArgumentType.integer(1, vaultLimit))
                                            .executes(ModCommandManager::openOtherVault))
                            ))
            );


            Command<ServerCommandSource> helpAction = context -> sendHelp(context.getSource(), 1);
            Command<ServerCommandSource> pageAction = context -> sendHelp(context.getSource(), IntegerArgumentType.getInteger(context, "page"));

            // 2. Build your new "help" node
            var myHelpNode = literal("help")
                    .executes(helpAction)
                    .then(argument("page", IntegerArgumentType.integer(1))
                            .executes(pageAction))
                    .build(); // .build() creates the node without registering it yet

            // 3. THE NINJA SWAP
            // We reach into the dispatcher's root and manually overwrite the "help" child
            dispatcher.getRoot().addChild(myHelpNode);

            // 4. Also register bdhelp just as a backup/alias
            dispatcher.register(literal("bdhelp")
                    .executes(helpAction)
                    .then(argument("page", IntegerArgumentType.integer(1))
                            .executes(pageAction)));

            dispatcher.register(literal("invsee")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(argument("target", StringArgumentType.string())
                            .executes(context -> {
                                ServerPlayerEntity admin = context.getSource().getPlayer();
                                String targetName = StringArgumentType.getString(context, "target");

                                // Lookup profile for offline support
                                var profileOpt = context.getSource().getServer().getUserCache().findByName(targetName);
                                if (profileOpt.isEmpty()) return 0;

                                openInvSee(admin, profileOpt.get());
                                return 1;
                            })));

            dispatcher.register(literal("sudo")
                    .then(argument("target", EntityArgumentType.player())
                            .then(argument("action", MessageArgumentType.message())
                                    .executes(ModCommandManager::sudo)
                            )
                    )
            );

            dispatcher.register(literal("admin").requires(s -> s.hasPermissionLevel(2))
                    .then(literal("shopbypass").executes(c -> {
                        UUID uuid = c.getSource().getPlayer().getUuid();
                        if (AdminStateRegistry.BYPASS_MODE.contains(uuid)) {
                            AdminStateRegistry.BYPASS_MODE.remove(uuid);
                            c.getSource().sendFeedback(() -> Text.literal("§eShop bypass §cdisabled§e."), false);
                        } else {
                            AdminStateRegistry.BYPASS_MODE.add(uuid);
                            c.getSource().sendFeedback(() -> Text.literal("§eShop bypass §aenabled§e. You can now open any shop chest."), false);
                        }
                        return 1;
                    })));

            dispatcher.register(literal("lock")
                    .requires(source -> source.hasPermissionLevel(0)) // Allow all players to lock their own stuff
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        UUID uuid = player.getUuid();

                        // Check if they are already in the "Waiting to click" state
                        if (LockEventHandler.IS_LOCKING.contains(uuid)) {
                            // If they run the command again, it cancels the mode
                            LockEventHandler.IS_LOCKING.remove(uuid);
                            context.getSource().sendFeedback(() -> Text.literal("§eLocking mode §ccancelled§e."), false);
                        } else {
                            // Otherwise, put them into the state
                            LockEventHandler.IS_LOCKING.add(uuid);
                            context.getSource().sendFeedback(() -> Text.literal("§6Locking Mode: §eRight-click a block to toggle its lock."), false);
                        }

                        return 1;
                    })
            );
        });
    }

    private static int sudo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target =  EntityArgumentType.getPlayer(context, "target");
        String action = MessageArgumentType.getMessage(context, "action").getString();
        String begin = action.toString().substring(0,2);
        String end = action.toString().substring(2);
        if (begin.equalsIgnoreCase("c:")) {
            Text authenticMessage = Text.literal("<")
                    .append(target.getDisplayName())
                    .append("> ")
                    .append(end);

            // 2. Broadcast to every player on the server
            for (ServerPlayerEntity onlinePlayer : target.getServer().getPlayerManager().getPlayerList()) {
                onlinePlayer.sendMessage(authenticMessage, false);
            }

            // 3. Log it to the console so it shows up in server logs like real chat
            target.getServer().sendMessage(authenticMessage);
        }
        else {
            forceCommand(target, action);
        }
        return 1;
    }

    public static void forceCommand(ServerPlayerEntity target, String command) {
        MinecraftServer server = target.getServer();
        if (server != null) {
            // This runs the command AS the player
            server.getCommandManager().executeWithPrefix(target.getCommandSource(), command);
        }
    }

    private static void openInvSee(ServerPlayerEntity admin, GameProfile targetProfile) {
        MinecraftServer server = admin.getServer();
        UUID targetUuid = targetProfile.getId();

        ServerPlayerEntity targetEntity = server.getPlayerManager().getPlayer(targetUuid);
        Inventory targetInv;

        // We format the title as "InvSee | Name" so the Client can parse the name
        // and fetch the skin/head automatically.
        Text title = Text.literal("InvSee | " + targetProfile.getName());

        if (targetEntity != null) {
            // ONLINE CASE
            targetInv = targetEntity.getInventory();
        } else {
            // OFFLINE CASE
            NbtCompound nbt = VaultUtils.getOfflinePlayerData(server, targetUuid);

            // Safety: If the player has never joined, nbt might be null
            if (nbt == null) {
                admin.sendMessage(Text.literal("§cError: Could not find data for " + targetProfile.getName()), false);
                return;
            }

            targetInv = VaultUtils.createOfflineInventory(nbt);

            // Save logic: Every change made to this SimpleInventory is written to the .dat file
            if (targetInv instanceof SimpleInventory offlineInv) {
                offlineInv.addListener(inventory -> {
                    VaultUtils.saveOfflinePlayerData(server, targetUuid, inventory);
                });
            }
        }

        // Open the screen with our custom injected title
        admin.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, player) ->
                new InvSeeScreenHandler(syncId, inv, targetInv), title));
    }

    private static int sendHelp(ServerCommandSource source, int page) {
        // 1. Get all commands the player has permission to see
        CommandDispatcher<ServerCommandSource> dispatcher = source.getServer().getCommandManager().getDispatcher();
        List<String> commandList = new ArrayList<>(dispatcher.getRoot().getChildren().stream()
                .filter(node -> node.canUse(source))
                .map(CommandNode::getName)
                .sorted()
                .toList());

        int commandsPerPage = 8;
        int totalPages = (int) Math.ceil((double) commandList.size() / commandsPerPage);
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        // Header
        source.sendMessage(Text.literal("--- Help: Page " + page + " of " + totalPages + " ---").formatted(Formatting.GOLD));
        source.sendMessage(Text.literal("Type /help <page> to see more").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));

        // 2. Display commands for the current page
        int start = (page - 1) * commandsPerPage;
        int end = Math.min(start + commandsPerPage, commandList.size());

        for (int i = start; i < end; i++) {
            String cmdName = commandList.get(i);

            // Custom styling for BD commands vs Vanilla
            Formatting cmdColor = isBDCommand(cmdName) ? Formatting.YELLOW : Formatting.WHITE;

            MutableText line = Text.literal("/" + cmdName)
                    .formatted(cmdColor)
                    .append(Text.literal(" - ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal("Click to suggest").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));

            // Add Click/Hover interaction
            line.styled(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + cmdName + " "))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§7Suggests: §e/" + cmdName))));

            source.sendMessage(line);
        }
        final int finalPage = page;
        // 3. Footer with clickable arrows
        MutableText footer = Text.literal("« Previous").formatted(page > 1 ? Formatting.AQUA : Formatting.GRAY);
        if (page > 1) {
            footer.styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bdhelp " + (finalPage - 1))));
        }

        footer.append(Text.literal(" | ").formatted(Formatting.GRAY));

        MutableText next = Text.literal("Next »").formatted(page < totalPages ? Formatting.AQUA : Formatting.GRAY);
        if (page < totalPages) {
            next.styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bdhelp " + (finalPage + 1))));
        }

        source.sendMessage(footer.append(next));
        return 1;
    }

    // Simple helper to highlight your specific mod commands
    private static boolean isBDCommand(String name) {
        List<String> myCommands = List.of("pv", "tpa", "tpahere", "home", "sethome", "nick", "bd");
        return myCommands.contains(name.toLowerCase());
    }

    private static int openOtherVault(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        int number = IntegerArgumentType.getInteger(context, "vaultNumber");

        MinecraftServer server = source.getServer();
        ServerPlayerEntity admin = source.getPlayer(); // The person typing the command

        // 1. Find the UUID from the Name (works for offline players)
        var profile = server.getUserCache().findByName(targetName);
        if (profile.isEmpty()) {
            source.sendError(Text.literal("Player not found in records!").formatted(Formatting.RED));
            return 0;
        }

        UUID targetUuid = profile.get().getId();
        String actualName = profile.get().getName();

        // 2. Open the vault using the Admin-specific method
        adminOpenVault(admin, targetUuid, actualName, number);

        return 1;
    }


    private static int openPVault(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        Integer number = IntegerArgumentType.getInteger(context, "vaultNumber");
        openVault(player, number);
        return 1;
    }

    private static void adminOpenVault(ServerPlayerEntity admin, UUID targetUuid, String targetName, int vaultNum) {
        MinecraftServer server = admin.getServer();

        // 1. Load the target's inventory using the UUID helper we made in VaultUtils
        SimpleInventory vaultInv = VaultUtils.loadVaultByUuid(server, targetUuid, vaultNum);

        // 2. Open the screen for the ADMIN
        admin.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                // Clearly label that this is someone else's vault
                return Text.literal(targetName + "'s Vault #" + vaultNum);
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory adminInv, PlayerEntity player) {
                return new PlayerVaultScreenHandler(syncId, adminInv, vaultInv);
            }
        });

        // 3. Save back to the TARGET'S data when the admin closes the screen
        vaultInv.addListener(inventory -> VaultUtils.saveVaultByUuid(server, targetUuid, vaultNum, (SimpleInventory) inventory));
    }

    private static void openVault(ServerPlayerEntity player, int vaultNum) {
        SimpleInventory vaultInv = VaultUtils.loadVault(player, vaultNum);

        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("Vault #" + vaultNum);
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity playerEntity) {
                // Pass the vaultInv (the 54-slot SimpleInventory) here
                return new PlayerVaultScreenHandler(syncId, playerInv, vaultInv);
            }
        });

        // We need a listener to save the vault when the player closes it
        vaultInv.addListener(inventory -> VaultUtils.saveVault(player, vaultNum, (SimpleInventory) inventory));
    }


    private static int setNickname(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        String newNick = StringArgumentType.getString(context, "nick");
        PlayerDataModel data = LocationStorageLib.getPlayerData(context.getSource().getServer(), target.getUuid());
        // 1. Clean up the input
        if (newNick.trim().isEmpty()) {
            data.nickname = ""; // Reset to empty
            LocationStorageLib.savePlayerData(context.getSource().getServer(), target.getUuid(), data);
            context.getSource().sendError(Text.literal("Nickname reset!"));
            return 0;
        }
        String stripped = newNick.replaceAll("&#[A-Fa-f0-9]{6}", "").replaceAll("&[0-9a-fk-or]", "");
        if (stripped.length() > 32) {
            context.getSource().sendError(Text.literal("The visible nickname is too long (max 32 characters)!"));
            return 0;
        }

        data.nickname = newNick;
        LocationStorageLib.savePlayerData(context.getSource().getServer(), target.getUuid(), data);

        // 3. Feedback using your True Color logic
        context.getSource().sendMessage(Text.literal("§6Nickname for §e" + target.getEntityName() + " §6set to: ")
                .append(ColorUtils.format(newNick)));

        return 1;
    }

    private static int whereami(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        player.sendMessage(Text.literal("You are in " + player.getWorld().getRegistryKey().getValue().toString() + " located at X:" + Math.round(player.getX()) + " Y:" + Math.round(player.getY()) + " Z:" + Math.round(player.getZ())).formatted(Formatting.GOLD));
        return 1;
    }

    private static int sendIdMessage(ServerCommandSource source, ItemStack stack) {
            String fullName = Registries.ITEM.getId(stack.getItem()).toString();
            DynamicIdMapper mapper = DynamicIdMapper.getServerState(source.getServer()  );

            // 3. Get or Create the ID (This replaces your old getIndex call)
            int id = mapper.getOrCreateIndex(fullName);

            source.sendFeedback(() -> MessageLib.idMessage(fullName, id), false);
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

        for (BalEntry e : allBalances) totalServerBalance += e.balance;

        allBalances.sort((a, b) -> Long.compare(b.balance, a.balance));

        int totalPlayers = allBalances.size();
        int maxPages = (int) Math.ceil((double) totalPlayers / PAGE_SIZE);

        if (page > maxPages && maxPages > 0) {
            source.sendFeedback(() -> Text.literal("The balance top only has " + maxPages + " pages.").formatted(Formatting.RED), false);
            return 0;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm aa");
        String timestamp = sdf.format(new Date());

        source.sendFeedback(() -> Text.literal("Top balances (" + timestamp + ")").formatted(Formatting.GOLD), false);

        source.sendFeedback(() -> MessageLib.baltopHeader(timestamp, page, maxPages), false);

        // --- SERVER TOTAL (Fixed Casting) ---
        long finalTotal = totalServerBalance;
        source.sendFeedback(() -> MessageLib.serverTotal(finalTotal), false);

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

    private static int pay(ServerCommandSource source, ServerPlayerEntity target, long amount) {
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

    private static int execute(CommandContext<ServerCommandSource> context) {
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

    private static int toggleTpa(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());
        data.tpaEnabled = !data.tpaEnabled; // Flip the switch
        data.lastKnownName = player.getName().getString();
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        player.sendMessage(MessageLib.tpaStatus(data.tpaEnabled));
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
        TeleportRequest request = new TeleportRequest(
                requester.getUuid(), Vec3d.ZERO, 0, 0, null, false
        );

        pendingRequests.put(targetUuid, request);
        target.sendMessage(MessageLib.tpaRequestReceived(requester.getName().getString()));
        requester.sendMessage(MessageLib.tpaRequestSent(target.getName().getString()));

        return 1;
    }

    private static int sendMail(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "targetName");
        String message = MessageArgumentType.getMessage(context, "msg").getString();
        MinecraftServer server = context.getSource().getServer();
        ServerCommandSource source = context.getSource();

        // 1. Find the UUID from the Name (works for offline players)
        var profile = server.getUserCache().findByName(targetName);
        if (profile.isEmpty()) {
            source.sendError(Text.literal("Player not found in records!").formatted(Formatting.RED));
            return 0;
        }
        UUID targetUuid = profile.get().getId();

        // 2. Load their data (even if they are offline)
        PlayerDataModel targetData = LocationStorageLib.getPlayerData(server, targetUuid);

        // 3. Add the mail with a timestamp or sender name
        String senderName = source.getName();
        targetData.mail.add("§e[" + senderName + "]: §f" + message);

        // 4. Save the data back to the file
        LocationStorageLib.savePlayerData(server, targetUuid, targetData);

        source.sendFeedback(() -> Text.literal("Mail sent to " + targetName + "!").formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int tpo(CommandContext<ServerCommandSource> context) {
        String targetName = StringArgumentType.getString(context, "targetName");
        MinecraftServer server = context.getSource().getServer();
        ServerCommandSource source = context.getSource();

        // 1. Find the UUID from the Name (works for offline players)
        var profile = server.getUserCache().findByName(targetName);
        if (profile.isEmpty()) {
            source.sendError(Text.literal("Player not found in records!").formatted(Formatting.RED));
            return 0;
        }
        UUID targetUuid = profile.get().getId();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, targetUuid);
        Identifier dimIdentifier = new Identifier(data.worldId);
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, dimIdentifier);
        ServerWorld targetWorld = server.getWorld(key);

        if (targetWorld != null) {
            // Now you can safely use the world!
            source.getPlayer().teleport(targetWorld, data.coords.x, data.coords.y, data.coords.z, data.lastYaw, data.lastPitch);
            source.sendMessage(Text.literal("Teleported to: " + targetName).formatted(Formatting.GOLD));
        }

        return 1;
    }


    private static int seen(CommandContext<ServerCommandSource> context) {
        String targetName = StringArgumentType.getString(context, "targetName");
        MinecraftServer server = context.getSource().getServer();
        ServerCommandSource source = context.getSource();

        // 1. Get the UUID and Data
        var profile = server.getUserCache().findByName(targetName);
        if (profile.isEmpty()) {
            source.sendError(Text.literal("Player not found in records."));
            return 0;
        }

        UUID uuid = profile.get().getId();

        // Check if they are ONLINE first
        ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(uuid);
        if (onlinePlayer != null) {
            source.sendMessage(Text.literal("§6" + targetName + " §7is currently §aonline§7!"));
            return 1;
        }

        PlayerDataModel data = LocationStorageLib.getPlayerData(server, uuid);

        // 2. Safety check for the timestamp
        if (data == null || data.lastLogonTime == null) {
            source.sendError(Text.literal("No login history found for this player."));
            return 0;
        }

        // 3. Calculate Difference
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime then = LocalDateTime.parse(data.lastLogonTime);

        // Years, Months, Days
        Period period = Period.between(then.toLocalDate(), now.toLocalDate());
        // Hours, Minutes, Seconds
        Duration duration = Duration.between(then, now);

        long h = duration.toHours() % 24;
        long m = duration.toMinutes() % 60;
        long s = duration.getSeconds() % 60;

        // 4. Build readable string
        StringBuilder sb = new StringBuilder();
        if (period.getYears() > 0) sb.append(period.getYears()).append("y ");
        if (period.getMonths() > 0) sb.append(period.getMonths()).append("m ");
        if (period.getDays() > 0) sb.append(period.getDays()).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0) sb.append(s).append("s ");

        String timeAgo = !sb.isEmpty() ? sb.toString().trim() + " ago" : "just now";

        source.sendMessage(Text.literal("§e" + targetName + " §6was last seen §e" + timeAgo));

        return 1;
    }

    private static int tpohere(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "targetName");
        MinecraftServer server = context.getSource().getServer();
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity sender = source.getPlayerOrThrow(); // The person running the command

        // 1. Find the UUID of the offline player
        var profile = server.getUserCache().findByName(targetName);
        if (profile.isEmpty()) {
            source.sendError(Text.literal("Player not found in records!").formatted(Formatting.RED));
            return 0;
        }

        UUID targetUuid = profile.get().getId();

        // 2. Load their existing data
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, targetUuid);

        // 3. Overwrite their data with YOUR current location
        data.coords = sender.getPos();
        data.lastYaw = sender.getYaw();
        data.lastPitch = sender.getPitch();
        data.worldId = sender.getWorld().getRegistryKey().getValue().toString();

        // 4. Save the modified data back to the file
        LocationStorageLib.savePlayerData(server, targetUuid, data);

        source.sendMessage(Text.literal("Success! " + targetName + " will spawn at your location upon login.")
                .formatted(Formatting.GOLD));

        return 1;
    }

    private static int readMail(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        if (data.mail.isEmpty()) {
            player.sendMessage(Text.literal("§6§lMail » §fYour inbox is empty."), false);
            return 1;
        }

        player.sendMessage(Text.literal("§6--- Your Mailbox ---").formatted(Formatting.BOLD), false);
        for (String msg : data.mail) {
            player.sendMessage(Text.literal(msg), false);
        }
        player.sendMessage(Text.literal("§6--------------------"), false);
        player.sendMessage(Text.literal("§eUse /mail clear to empty your inbox."), false);

        return 1;
    }

    private static int clearMail(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        data.mail.clear(); // Wipe the list
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        player.sendMessage(Text.literal("§6§lMail » §aInbox cleared!").formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int msg(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayer();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        String msg = MessageArgumentType.getMessage(context, "msg").getString();
        if (requester == null || target == null) return 0;
        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, target.getUuid());
        data.lastMessenger = requester.getUuid();
        LocationStorageLib.savePlayerData(server, target.getUuid(), data);
        if (requester == target) {
            context.getSource().sendError(Text.literal("You can't just msg yourself dingaling!").formatted(Formatting.RED));
            return 0;
        }
        target.sendMessage(MessageLib.msgReceive(requester.getName().getString(), msg));
        requester.sendMessage(MessageLib.msgSend(target.getName().getString(), msg));
        return 1;
    }

    private static int respond(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayer();
        if (requester == null) return 0;

        MinecraftServer server = context.getSource().getServer();

        // 1. Get MY data to see who messaged me last
        PlayerDataModel myData = LocationStorageLib.getPlayerData(server, requester.getUuid());
        UUID targetUuid = myData.lastMessenger;

        if (targetUuid == null) {
            context.getSource().sendError(Text.literal("Nobody has messaged you yet!"));
            return 0;
        }

        // 2. Find that player on the server
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
        if (target == null) {
            context.getSource().sendError(Text.literal("That player is no longer online!"));
            return 0;
        }

        // 3. Get the message
        String msg = MessageArgumentType.getMessage(context, "msg").getString();

        // --- THE PING-PONG FIX ---
        // Update the TARGET'S data so they can now use /r to reply to ME
        PlayerDataModel targetData = LocationStorageLib.getPlayerData(server, target.getUuid());
        targetData.lastMessenger = requester.getUuid();
        // Save their data so it persists
        LocationStorageLib.savePlayerData(server, target.getUuid(), targetData);
        // -------------------------

        // 4. Send the visual feedback
        target.sendMessage(MessageLib.msgReceive(requester.getName().getString(), msg));
        requester.sendMessage(MessageLib.msgSend(target.getName().getString(), msg));

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

    private static int tpaccept(CommandContext<ServerCommandSource> context) {
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

    private static int executeWarp(CommandContext<ServerCommandSource> context) {
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

        player.sendMessage(MessageLib.warpingTo(name));
        return 1;
    }

    private static int setWarp(CommandContext<ServerCommandSource> context) {
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

        player.sendMessage(MessageLib.warpSet(warpName));

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestWarps(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();

        // Suggest BD Warps
        LocationStorageLib.getGlobalWarps(server).warps.keySet().forEach(builder::suggest);

        // Suggest Waystones
        if (FabricLoader.getInstance().isModLoaded("waystones")) {
            LocationStorageLib.getWaystoneWarps(server).keySet().forEach(builder::suggest);
        }

        return builder.buildFuture();
    }

    private static int tpdeny(CommandContext<ServerCommandSource> context) {
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

    private static int listHomes(CommandContext<ServerCommandSource> context) {
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

    private static int tpHome(CommandContext<ServerCommandSource> context) {
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

    private static int delHome(CommandContext<ServerCommandSource> context) {
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

    private static int setHome(CommandContext<ServerCommandSource> context) {
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

    private static int setHomeDefault(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        // Logic: auto-incrementing name
        String homeName = "home_" + (data.homes.size() + 1);

        if (data.homes.size() >= ModConfigHelper.get().homeLimit && !data.homes.containsKey(homeName)) {
            player.sendMessage(MessageLib.homeLimitReached(ModConfigHelper.get().homeLimit));
            return 0;
        }

        BDLocation loc = new BDLocation(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey().getValue().toString()
        );

        data.homes.put(homeName, loc);
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        // Use the specific default message
        player.sendMessage(MessageLib.homeSetDefault(homeName));
        return 1;
    }

    private static int executeBack(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        if (data.lastLocation == null) {
            player.sendMessage(MessageLib.BACK_NO_LOCATION);
            return 0;
        }

        BDLocation backLoc = data.lastLocation;

        // Save current spot before jumping (the "toggle" logic)
        LocationStorageLib.saveBackLocation(player);

        player.teleport(
                LocationStorageLib.getWorldFromString(player, backLoc.worldId),
                backLoc.x, backLoc.y, backLoc.z,
                backLoc.yaw, backLoc.pitch
        );

        player.sendMessage(MessageLib.BACK_SUCCESS);
        return 1;
    }
}