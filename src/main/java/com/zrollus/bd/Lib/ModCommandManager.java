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
import com.zrollus.bd.shopkeeper.ShopkeeperCommands;
import com.zrollus.bd.essentials.EssentialsManager;
import com.zrollus.bd.essentials.EssentialsSystem;
import com.zrollus.bd.essentials.TeleportService;
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

    private static final HashMap<UUID, LinkedHashMap<UUID, TeleportRequest>> pendingRequests = new HashMap<>();
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
                    .requires(s -> PermissionCompat.check(s, "command.tpahere", 0))
                    .then(argument("target", EntityArgumentType.player())
                            .executes(ModCommandManager::tpahere)));

            dispatcher.register(literal("tpa")
                    .requires(s -> PermissionCompat.check(s, "command.tpa", 0))
                    .then(argument("target", EntityArgumentType.player())
                            .executes(ModCommandManager::tpa)));

            dispatcher.register(literal("tpaccept")
                    .requires(s -> PermissionCompat.check(s, "command.tpaccept", 0))
                    .executes(ModCommandManager::tpaccept)
                    .then(argument("player", EntityArgumentType.player()).executes(ModCommandManager::tpaccept)));

            dispatcher.register(literal("tpdeny")
                    .requires(s -> PermissionCompat.check(s, "command.tpdeny", 0))
                    .executes(ModCommandManager::tpdeny)
                    .then(argument("player", EntityArgumentType.player()).executes(ModCommandManager::tpdeny))
            );

            dispatcher.register(literal("tpyes")
                    .requires(s -> PermissionCompat.check(s, "command.tpaccept", 0))
                    .executes(ModCommandManager::tpaccept)
                    .then(argument("player", EntityArgumentType.player()).executes(ModCommandManager::tpaccept)));

            dispatcher.register(literal("tpno")
                    .requires(s -> PermissionCompat.check(s, "command.tpdeny", 0))
                    .executes(ModCommandManager::tpdeny)
                    .then(argument("player", EntityArgumentType.player()).executes(ModCommandManager::tpdeny))
            );

            dispatcher.register(literal("tpacancel")
                    .requires(s -> PermissionCompat.check(s, "command.tpacancel", 0))
                    .executes(ModCommandManager::cancelTeleportRequests)
                    .then(argument("player", EntityArgumentType.player()).executes(ModCommandManager::cancelTeleportRequests)));

            dispatcher.register(literal("tpatoggle")
                    .requires(s -> PermissionCompat.check(s, "command.tptoggle", 0))
                    .executes(ModCommandManager::toggleTpa));

            dispatcher.register(literal("homes")
                    .requires(s -> PermissionCompat.check(s, "command.homes", 0))
                    .executes(ModCommandManager::listHomes));

            dispatcher.register(literal("home")
                    .requires(s -> PermissionCompat.check(s, "command.home", 0))
                    .then(argument("name", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                MinecraftServer server = context.getSource().getServer();
                                PlayerDataModel data = LocationStorageLib.getPlayerData(server, context.getSource().getPlayer().getUuid());
                                data.homes.keySet().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ModCommandManager::tpHome)));

            dispatcher.register(literal("delhome")
                    .requires(s -> PermissionCompat.check(s, "command.delhome", 0))
                    .then(argument("name", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                MinecraftServer server = context.getSource().getServer();
                                PlayerDataModel data = LocationStorageLib.getPlayerData(server, context.getSource().getPlayer().getUuid());
                                data.homes.keySet().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ModCommandManager::delHome)));

            dispatcher.register(literal("sethome")
                    .requires(s -> PermissionCompat.check(s, "command.sethome", 0))
                    .then(argument("name", StringArgumentType.string())
                            .executes(ModCommandManager::setHome))
                    .executes(ModCommandManager::setHomeDefault));

            dispatcher.register(literal("back")
                    .requires(s -> PermissionCompat.check(s, "command.back", 0))
                    .executes(ModCommandManager::executeBack));

            // Inside register()
            dispatcher.register(literal("warp")
                    .requires(s -> PermissionCompat.check(s, "command.warp", 0))
                    .then(argument("name", StringArgumentType.string())
                            .suggests(ModCommandManager::suggestWarps) // We'll build this next
                            .executes(ModCommandManager::executeWarp)));

            dispatcher.register(literal("setwarp")
                    .requires(source -> PermissionCompat.check(source, "command.setwarp", 2))
                    .then(argument("name", StringArgumentType.string())
                            .executes(ModCommandManager::setWarp)));


            dispatcher.register(CommandManager.literal("bal")
                    .requires(s -> PermissionCompat.check(s, "command.balance", 0))
                    // Base command: /bal (shows self)
                    .executes(context -> showBalance(context.getSource(), context.getSource().getPlayerOrThrow()))

                    // Administrative Subcommands
                    .then(CommandManager.literal("set")
                            .requires(s -> PermissionCompat.check(s, "command.eco.set", 2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(0))
                                            .executes(context -> modifyBalance(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount"), "set")))))

                    .then(CommandManager.literal("add")
                            .requires(s -> PermissionCompat.check(s, "command.eco.give", 2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                            .executes(context -> modifyBalance(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount"), "add")))))

                    .then(CommandManager.literal("remove")
                            .requires(s -> PermissionCompat.check(s, "command.eco.take", 2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                            .executes(context -> modifyBalance(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount"), "remove")))))
            );
            dispatcher.register(CommandManager.literal("baltop")
                    .requires(s -> PermissionCompat.check(s, "command.balancetop", 0))
                    .executes(context -> displayBalTop(context.getSource(), 1))
                    .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                            .executes(context -> displayBalTop(context.getSource(), IntegerArgumentType.getInteger(context, "page"))))
            );

            dispatcher.register(CommandManager.literal("pay")
                .requires(s -> PermissionCompat.check(s, "command.pay", 0))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> pay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), LongArgumentType.getLong(context, "amount")))))
            );

            dispatcher.register(literal("id")
                    .requires(s -> PermissionCompat.check(s, "command.itemdb", 0))
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
                .requires(s -> PermissionCompat.check(s, "command.msg", 0))
                .then(argument("target", EntityArgumentType.player())
                    .then(argument("msg", MessageArgumentType.message())
                            .executes(ModCommandManager::msg)
                    )
                )
            );
            for (String alias : List.of("msg", "tell", "whisper", "pm", "w"))
                dispatcher.register(literal(alias).redirect(dispatcher.getRoot().getChild("emsg")));

            dispatcher.register(literal("r")
                .requires(s -> PermissionCompat.check(s, "command.reply", 0))
                .then(argument("msg", MessageArgumentType.message())
                        .executes(ModCommandManager::respond)
                )
            );

            dispatcher.register(literal("mail")
                .requires(s -> PermissionCompat.check(s, "command.mail", 0))
                .then(literal("read").executes(c -> readMail(c, 1))
                        .then(argument("page", IntegerArgumentType.integer(1)).executes(c -> readMail(c, IntegerArgumentType.getInteger(c, "page")))))
                .then(literal("clear").executes(ModCommandManager::clearMail)
                        .then(argument("number", IntegerArgumentType.integer(1)).executes(ModCommandManager::clearOneMail)))
                .then(literal("send")
                    .then(argument("targetName", StringArgumentType.string())
                        .then(argument("msg", MessageArgumentType.message())
                            .executes(ModCommandManager::sendMail))))
                .then(literal("sendtemp")
                        .then(argument("targetName", StringArgumentType.string())
                                .then(argument("duration", StringArgumentType.word())
                                        .then(argument("msg", MessageArgumentType.message()).executes(ModCommandManager::sendTemporaryMail)))))
                .then(literal("sendall").requires(s -> PermissionCompat.check(s, "command.mail.sendall", 2))
                        .then(argument("msg", MessageArgumentType.message()).executes(ModCommandManager::sendAllMail))));

            dispatcher.register(literal("whereami")
                .requires(s -> PermissionCompat.check(s, "command.getpos", 0))
                .executes(ModCommandManager::whereami)
            );

            dispatcher.register(literal("tpo")
                    .requires(s -> PermissionCompat.check(s, "command.tpo", 2))
                    .then(argument("targetName", StringArgumentType.string())
                            .executes(ModCommandManager::tpo)));

            dispatcher.register(literal("tpohere")
                    .requires(s -> PermissionCompat.check(s, "command.tpohere", 2))
                    .then(argument("targetName", StringArgumentType.string())
                            .executes(ModCommandManager::tpohere)));

            dispatcher.register(literal("seen")
                    .requires(s -> PermissionCompat.check(s, "command.seen", 0))
                    .then(argument("targetName", StringArgumentType.string())
                            .executes(ModCommandManager::seen)));

            dispatcher.register(literal("nick")
                            .requires(s -> PermissionCompat.check(s, "command.nick.others", 2))
                            .then(argument("player", EntityArgumentType.player())
                                    .then(argument("nick", StringArgumentType.greedyString())
                                            .executes(ModCommandManager::setNickname)
                            )));

            dispatcher.register(literal("nickname")
                    .requires(s -> PermissionCompat.check(s, "command.nick.others", 2))
                    .then(argument("player", EntityArgumentType.player())
                            .then(argument("nick", StringArgumentType.greedyString())
                                    .executes(ModCommandManager::setNickname)
                            )));
            int vaultLimit = ModConfigHelper.get().maxVaults;

            dispatcher.register(literal("pv")
                    .requires(s -> PermissionCompat.check(s, "command.playervault", 0))
                    // 1. Path for self: /pv <number>
                    .then(argument("vaultNumber", IntegerArgumentType.integer(1, vaultLimit))
                            .executes(ModCommandManager::openPVault))

                    // 2. Path for admins: /pv open <player> <number>
                    .then(literal("open")
                            .requires(s -> PermissionCompat.check(s, "command.playervault.others", 2))
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
                    .then(argument("command", StringArgumentType.greedyString())
                            .executes(context -> sendCommandHelp(context.getSource(), StringArgumentType.getString(context, "command"))))
                    .build(); // .build() creates the node without registering it yet

            // 3. THE NINJA SWAP
            // We reach into the dispatcher's root and manually overwrite the "help" child
            dispatcher.getRoot().addChild(myHelpNode);

            // 4. Also register bdhelp just as a backup/alias
            dispatcher.register(literal("bdhelp")
                    .executes(helpAction)
                    .then(argument("page", IntegerArgumentType.integer(1))
                            .executes(pageAction))
                    .then(argument("command", StringArgumentType.greedyString())
                            .executes(context -> sendCommandHelp(context.getSource(), StringArgumentType.getString(context, "command")))));

            dispatcher.register(literal("invsee")
                    .requires(s -> PermissionCompat.check(s, "command.invsee", 2))
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
                    .requires(s -> PermissionCompat.check(s, "command.sudo", 2))
                    .then(argument("target", EntityArgumentType.player())
                            .then(argument("action", MessageArgumentType.message())
                                    .executes(ModCommandManager::sudo)
                            )
                    )
            );

            dispatcher.register(literal("admin").requires(s -> PermissionCompat.check(s, "command.admin", 2))
                    .then(literal("shopbypass").executes(c -> {
                        UUID uuid = c.getSource().getPlayer().getUuid();
                        if (AdminStateRegistry.BYPASS_MODE.contains(uuid)) {
                            AdminStateRegistry.BYPASS_MODE.remove(uuid);
                            c.getSource().sendFeedback(() -> ItemNameCommand.parseLegacyFormatting("&eShop bypass &cdisabled&e."), false);
                        } else {
                            AdminStateRegistry.BYPASS_MODE.add(uuid);
                            c.getSource().sendFeedback(() -> ItemNameCommand.parseLegacyFormatting("&eShop bypass &aenabled&e. You can now open any shop chest."), false);
                        }
                        return 1;
                    })));

            dispatcher.register(literal("lock")
                    .requires(source -> PermissionCompat.check(source, "command.lock", 0))
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        UUID uuid = player.getUuid();

                        // Check if they are already in the "Waiting to click" state
                        if (LockEventHandler.IS_LOCKING.contains(uuid)) {
                            // If they run the command again, it cancels the mode
                            LockEventHandler.IS_LOCKING.remove(uuid);
                            context.getSource().sendFeedback(() -> ItemNameCommand.parseLegacyFormatting("&eLocking mode &ccancelled&e."), false);
                        } else {
                            // Otherwise, put them into the state
                            LockEventHandler.IS_LOCKING.add(uuid);
                            context.getSource().sendFeedback(() -> ItemNameCommand.parseLegacyFormatting("&6Locking Mode: &eRight-click a block to toggle its lock."), false);
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
                admin.sendMessage(ItemNameCommand.parseLegacyFormatting("&cError: Could not find data for " + targetProfile.getName()), false);
                return;
            }

            targetInv = VaultUtils.createOfflineInventory(server, nbt);

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
                .filter(node -> !node.getName().equalsIgnoreCase("bdrel"))
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
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ItemNameCommand.parseLegacyFormatting("&7Suggests: &e/" + cmdName))));

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

    private static int sendCommandHelp(ServerCommandSource source, String input) {
        String query = input == null ? "" : input.trim();
        if (query.startsWith("/")) query = query.substring(1);
        if (query.matches("\\d+")) return sendHelp(source, Integer.parseInt(query));

        String commandName = query.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (commandName.equals("bdrel")) {
            source.sendError(Text.literal("No help entry is available for /bdrel."));
            return 0;
        }
        if (commandName.equals("shopkeeper") || commandName.equals("shopkeepers") || commandName.equals("sk")) {
            ShopkeeperCommands.sendDetailedHelp(source);
            return 1;
        }

        CommandDispatcher<ServerCommandSource> dispatcher = source.getServer().getCommandManager().getDispatcher();
        CommandNode<ServerCommandSource> node = dispatcher.getRoot().getChild(commandName);
        if (node == null || !node.canUse(source)) {
            source.sendError(Text.literal("No command named /" + commandName + " is available to you."));
            return 0;
        }
        while (node.getRedirect() != null) node = node.getRedirect();

        source.sendMessage(Text.literal("--- Help: /" + commandName + " ---").formatted(Formatting.GOLD, Formatting.BOLD));
        source.sendMessage(Text.literal(commandDescription(commandName)).formatted(Formatting.GRAY));

        Map<CommandNode<ServerCommandSource>, String> usages = dispatcher.getSmartUsage(node, source);
        if (node.getCommand() != null) sendUsageLine(source, "/" + commandName);
        if (usages.isEmpty() && node.getCommand() == null) {
            source.sendMessage(Text.literal("No usable subcommands are available with your permissions.").formatted(Formatting.RED));
            return 0;
        }
        for (String usage : usages.values()) sendUsageLine(source, "/" + commandName + " " + usage);
        return 1;
    }

    private static void sendUsageLine(ServerCommandSource source, String usage) {
        MutableText line = Text.literal(usage).formatted(Formatting.YELLOW)
                .append(Text.literal(" - Click to use").formatted(Formatting.GRAY));
        line.styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, usage + (usage.endsWith("]") || usage.endsWith(">") ? "" : " ")))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Suggest " + usage))));
        source.sendMessage(line);
    }

    private static String commandDescription(String command) {
        return switch (command) {
            case "help", "bdhelp" -> "Lists commands or shows detailed help for one command.";
            case "itemname" -> "Renames the held item and supports legacy & color and style codes.";
            case "pv" -> "Opens one of your player vaults; operators can inspect another player's vault.";
            case "invsee" -> "Lets operators inspect and edit a player's inventory.";
            case "home", "homes", "sethome", "delhome" -> "Manages saved player home locations.";
            case "warp", "warps", "setwarp", "delwarp" -> "Manages shared server warp locations.";
            case "tpa", "tpahere", "tpaccept", "tpdeny" -> "Manages player-to-player teleport requests.";
            case "back" -> "Returns you to your previous saved location.";
            case "spawn", "setspawn" -> "Teleports to or changes the server spawn point.";
            case "nick", "nickname" -> "Changes a player's displayed nickname.";
            case "give" -> "Gives an item to one or more players.";
            case "gamemode" -> "Changes a player's game mode.";
            case "teleport", "tp" -> "Teleports entities to another location or entity.";
            case "time" -> "Queries or changes the world's time.";
            case "weather" -> "Changes the world's weather.";
            case "effect" -> "Adds, removes, or clears status effects.";
            case "enchant" -> "Enchants the item held by a player.";
            case "clear" -> "Removes matching items from player inventories.";
            case "summon" -> "Creates an entity at a location.";
            case "kill" -> "Removes or kills the selected entities.";
            case "execute" -> "Runs another command with changed conditions, location, or executor.";
            case "scoreboard" -> "Manages scoreboard objectives, scores, and display slots.";
            case "team" -> "Manages scoreboard teams and their options.";
            case "gamerule" -> "Queries or changes world game rules.";
            case "difficulty" -> "Queries or changes the world's difficulty.";
            case "locate" -> "Finds nearby structures, biomes, or points of interest.";
            default -> "Shows the valid syntax and subcommands currently available for /" + command + ".";
        };
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
        context.getSource().sendMessage(ItemNameCommand.parseLegacyFormatting("&6Nickname for &e" + target.getName().getString() + " &6set to: ")
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
        EssentialsManager essentials = EssentialsManager.get(server);
        if (!essentials.player(target.getUuid()).paymentsEnabled
                && !PermissionCompat.check(source, "command.pay.bypass", 2)) {
            source.sendError(Text.literal(target.getName().getString() + " is not accepting payments."));
            return 0;
        }
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
        essentials.logTransaction("pay ₱" + amount + " " + source.getPlayer().getName().getString() + " -> " + target.getName().getString());
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

        if (!data.tpaEnabled || !EssentialsManager.get(server).player(target.getUuid()).teleportEnabled) {
            context.getSource().sendError(Text.literal(target.getName().getString() + " has teleportation requests disabled.")
                    .formatted(Formatting.RED));
            return 0;
        }
        UUID targetUuid = target.getUuid();
        TeleportRequest request = new TeleportRequest(
                requester.getUuid(), Vec3d.ZERO, 0, 0, null, false
        );

        if (EssentialsManager.get(server).player(targetUuid).autoTeleport) {
            LocationStorageLib.saveBackLocation(requester);
            TeleportService.teleport(requester, target.getServerWorld(), target.getX(), target.getY(), target.getZ(),
                    target.getYaw(), target.getPitch(), true, false);
            return 1;
        }
        addTeleportRequest(server, targetUuid, request, requester);
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
        targetData.mail.add("&e[" + senderName + "]: &f" + mailBody(source, message));

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
        Identifier dimIdentifier = Identifier.of(data.worldId);
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
            source.sendMessage(ItemNameCommand.parseLegacyFormatting("&6" + targetName + " &7is currently &aonline&7!"));
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

        source.sendMessage(ItemNameCommand.parseLegacyFormatting("&e" + targetName + " &6was last seen &e" + timeAgo));

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

    private static int readMail(CommandContext<ServerCommandSource> context, int page) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        data.mail.removeIf(ModCommandManager::expiredMail);
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);
        if (data.mail.isEmpty()) {
            player.sendMessage(ItemNameCommand.parseLegacyFormatting("&6&lMail &r&7» &fYour inbox is empty."), false);
            return 1;
        }

        int pages = Math.max(1, (data.mail.size() + 7) / 8); page = Math.clamp(page, 1, pages);
        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&6&l--- Your Mailbox " + page + "/" + pages + " ---"), false);
        int start = (page - 1) * 8;
        for (int i = start; i < Math.min(start + 8, data.mail.size()); i++)
            player.sendMessage(Text.literal("#" + (i + 1) + " ").formatted(Formatting.DARK_GRAY)
                    .append(ItemNameCommand.parseLegacyFormatting(normalizeLegacyCodes(visibleMail(data.mail.get(i))))), false);
        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&e/mail clear <number> &7or &e/mail clear"), false);

        return 1;
    }

    private static int clearMail(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = context.getSource().getServer();
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        data.mail.clear(); // Wipe the list
        LocationStorageLib.savePlayerData(server, player.getUuid(), data);

        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&6&lMail &r&7» &aInbox cleared!"), false);
        return 1;
    }

    private static int msg(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayer();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        String msg = MessageArgumentType.getMessage(context, "msg").getString();
        if (requester == null || target == null) return 0;
        if (requester == target) {
            context.getSource().sendError(Text.literal("You can't just msg yourself dingaling!").formatted(Formatting.RED));
            return 0;
        }
        return deliverPrivateMessage(context, requester, target, msg);
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

        return deliverPrivateMessage(context, requester, target, msg);
    }

    private static int deliverPrivateMessage(CommandContext<ServerCommandSource> context,
                                             ServerPlayerEntity requester,
                                             ServerPlayerEntity target,
                                             String rawMessage) {
        MinecraftServer server = context.getSource().getServer();
        EssentialsManager manager = EssentialsManager.get(server);
        if (manager.isMuted(requester.getUuid())) {
            context.getSource().sendError(Text.literal("You are muted: " + manager.player(requester.getUuid()).muteReason));
            return 0;
        }
        if (!manager.player(target.getUuid()).messagesEnabled
                && !PermissionCompat.check(requester, "command.msg.bypass", requester.hasPermissionLevel(2))) {
            context.getSource().sendError(Text.literal(target.getName().getString() + " is not accepting private messages."));
            return 0;
        }
        if (manager.player(target.getUuid()).ignored.contains(requester.getUuid())) {
            context.getSource().sendError(Text.literal("That player is ignoring you."));
            return 0;
        }

        Text body = PermissionCompat.check(requester, "command.msg.color", false)
                ? ItemNameCommand.parseLegacyFormatting(rawMessage)
                : Text.literal(rawMessage);
        MutableText incoming = Text.literal("[").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(requester.getName().getString()).formatted(Formatting.GOLD))
                .append(Text.literal(" -> me] ").formatted(Formatting.DARK_GRAY)).append(body.copy());
        MutableText outgoing = Text.literal("[me -> ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(target.getName().getString()).formatted(Formatting.GOLD))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY)).append(body.copy());
        target.sendMessage(incoming, false);
        requester.sendMessage(outgoing, false);

        PlayerDataModel targetData = LocationStorageLib.getPlayerData(server, target.getUuid());
        targetData.lastMessenger = requester.getUuid();
        LocationStorageLib.savePlayerData(server, target.getUuid(), targetData);
        EssentialsSystem.markActive(requester);
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            if (viewer == requester || viewer == target || !manager.player(viewer.getUuid()).socialSpy) continue;
            viewer.sendMessage(Text.literal("[SocialSpy] ").formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(requester.getName().getString() + " -> " + target.getName().getString() + ": ").formatted(Formatting.GRAY))
                    .append(body.copy()), false);
        }
        return 1;
    }

    private static int clearOneMail(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer(); if (player == null) return 0;
        PlayerDataModel data = LocationStorageLib.getPlayerData(context.getSource().getServer(), player.getUuid());
        int index = IntegerArgumentType.getInteger(context, "number") - 1;
        if (index < 0 || index >= data.mail.size()) { context.getSource().sendError(Text.literal("That mail number does not exist.")); return 0; }
        data.mail.remove(index); LocationStorageLib.savePlayerData(context.getSource().getServer(), player.getUuid(), data);
        player.sendMessage(Text.literal("Mail deleted.").formatted(Formatting.GREEN)); return 1;
    }

    private static int sendTemporaryMail(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String duration = StringArgumentType.getString(context, "duration"); long millis = parseSimpleDuration(duration);
        if (millis <= 0) { context.getSource().sendError(Text.literal("Use a duration such as 10m, 2h, or 7d.")); return 0; }
        return storeMail(context, "@expires:" + (System.currentTimeMillis() + millis) + "|");
    }

    private static int sendAllMail(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String message = MessageArgumentType.getMessage(context, "msg").getString(); int sent = 0;
        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            PlayerDataModel data = LocationStorageLib.getPlayerData(context.getSource().getServer(), player.getUuid());
            data.mail.add("&e[" + context.getSource().getName() + "]: &f" + mailBody(context.getSource(), message));
            LocationStorageLib.savePlayerData(context.getSource().getServer(), player.getUuid(), data); sent++;
        }
        context.getSource().sendMessage(Text.literal("Mail sent to " + sent + " online player(s).").formatted(Formatting.GREEN)); return 1;
    }

    private static int storeMail(CommandContext<ServerCommandSource> context, String prefix) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "targetName"); String message = MessageArgumentType.getMessage(context, "msg").getString();
        MinecraftServer server = context.getSource().getServer(); var profile = server.getUserCache().findByName(targetName);
        if (profile.isEmpty()) { context.getSource().sendError(Text.literal("Player not found in records!")); return 0; }
        PlayerDataModel data = LocationStorageLib.getPlayerData(server, profile.get().getId());
        data.mail.add(prefix + "&e[" + context.getSource().getName() + "]: &f" + mailBody(context.getSource(), message));
        LocationStorageLib.savePlayerData(server, profile.get().getId(), data); return 1;
    }

    private static boolean expiredMail(String mail) { if (!mail.startsWith("@expires:")) return false; int split = mail.indexOf('|'); if (split < 10) return false; try { return Long.parseLong(mail.substring(9, split)) <= System.currentTimeMillis(); } catch (NumberFormatException ignored) { return false; } }
    private static String visibleMail(String mail) { int split = mail.startsWith("@expires:") ? mail.indexOf('|') : -1; return split >= 0 ? mail.substring(split + 1) : mail; }
    private static String normalizeLegacyCodes(String value) { return value.replace('\u00a7', '&').replace("Â&", "&"); }
    private static String mailBody(ServerCommandSource source, String message) {
        return PermissionCompat.check(source, "command.mail.color", 2) ? message : message.replace("&", "&&");
    }
    private static long parseSimpleDuration(String value) { try { long multiplier = switch (Character.toLowerCase(value.charAt(value.length() - 1))) { case 's' -> 1000L; case 'm' -> 60_000L; case 'h' -> 3_600_000L; case 'd' -> 86_400_000L; default -> -1L; }; return multiplier < 0 ? -1 : Math.multiplyExact(Long.parseLong(value.substring(0, value.length() - 1)), multiplier); } catch (RuntimeException ignored) { return -1; } }

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

        if (!data.tpaEnabled || !EssentialsManager.get(server).player(target.getUuid()).teleportEnabled) {
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
        if (EssentialsManager.get(server).player(targetUuid).autoTeleport) {
            LocationStorageLib.saveBackLocation(target);
            TeleportService.teleport(target, requester.getServerWorld(), requester.getX(), requester.getY(), requester.getZ(),
                    requester.getYaw(), requester.getPitch(), true, false);
            return 1;
        }
        addTeleportRequest(server, targetUuid, request, requester);
        target.sendMessage(MessageLib.tpaHereRequestReceived(requester.getName().getString()));
        requester.sendMessage(MessageLib.tpaHereRequestSent(target.getName().getString()));

        return 1;
    }

    private static int tpaccept(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity acceptor = context.getSource().getPlayer();
        if (acceptor == null) return 0;

        UUID requested = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("player"))
                ? EntityArgumentType.getPlayer(context, "player").getUuid() : null;
        TeleportRequest request = takeTeleportRequest(acceptor.getUuid(), requested);
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
            TeleportService.teleport(acceptor, acceptor.getServer().getWorld(request.dimension()),
                    request.pos().x, request.pos().y, request.pos().z, request.yaw(), request.pitch(), true, false);
            acceptor.sendMessage(Text.literal("Teleporting to " + requester.getName().getString()).formatted(Formatting.GOLD));
        } else {
            // CASE: /tpa -> Move the REQUESTER to the ACCEPTOR'S CURRENT position
            LocationStorageLib.saveBackLocation(requester);
            TeleportService.teleport(requester, acceptor.getServerWorld(), acceptor.getX(), acceptor.getY(), acceptor.getZ(),
                    acceptor.getYaw(), acceptor.getPitch(), true, false);
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

        TeleportService.teleport(player, LocationStorageLib.getWorldFromString(player, loc.worldId),
                loc.x, loc.y, loc.z, loc.yaw, loc.pitch, true, true);

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

    private static int tpdeny(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity acceptor = context.getSource().getPlayer();
        if (acceptor == null) return 0;

// Remove the request from the map
        UUID requested = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("player"))
                ? EntityArgumentType.getPlayer(context, "player").getUuid() : null;
        TeleportRequest request = takeTeleportRequest(acceptor.getUuid(), requested);

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

    private static void addTeleportRequest(MinecraftServer server, UUID target, TeleportRequest request,
                                           ServerPlayerEntity requester) {
        pendingRequests.computeIfAbsent(target, ignored -> new LinkedHashMap<>())
                .put(request.requesterUuid(), request);
        scheduler.schedule(() -> server.execute(() -> {
            LinkedHashMap<UUID, TeleportRequest> requests = pendingRequests.get(target);
            if (requests != null && request.equals(requests.remove(request.requesterUuid()))) {
                if (requests.isEmpty()) pendingRequests.remove(target);
                requester.sendMessage(MessageLib.TPA_TIMEOUT);
            }
        }), config.tpaRequestTimeout, TimeUnit.SECONDS);
    }

    private static TeleportRequest takeTeleportRequest(UUID target, UUID requester) {
        LinkedHashMap<UUID, TeleportRequest> requests = pendingRequests.get(target);
        if (requests == null || requests.isEmpty()) return null;
        UUID selected = requester;
        if (selected == null) for (UUID id : requests.keySet()) selected = id;
        TeleportRequest result = requests.remove(selected);
        if (requests.isEmpty()) pendingRequests.remove(target);
        return result;
    }

    private static int cancelTeleportRequests(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayerOrThrow();
        UUID target = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("player"))
                ? EntityArgumentType.getPlayer(context, "player").getUuid() : null;
        int removed = 0;
        for (var iterator = pendingRequests.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (target != null && !entry.getKey().equals(target)) continue;
            if (entry.getValue().remove(requester.getUuid()) != null) removed++;
            if (entry.getValue().isEmpty()) iterator.remove();
        }
        if (removed == 0) {
            context.getSource().sendError(Text.literal("You have no matching teleport requests to cancel."));
            return 0;
        }
        requester.sendMessage(Text.literal("Cancelled " + removed + " teleport request(s).").formatted(Formatting.YELLOW));
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

        TeleportService.teleport(player, LocationStorageLib.getWorldFromString(player, loc.worldId),
                loc.x, loc.y, loc.z, loc.yaw, loc.pitch, true, true);

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

        TeleportService.teleport(player, LocationStorageLib.getWorldFromString(player, backLoc.worldId),
                backLoc.x, backLoc.y, backLoc.z, backLoc.yaw, backLoc.pitch, true, true);

        player.sendMessage(MessageLib.BACK_SUCCESS);
        return 1;
    }
}
