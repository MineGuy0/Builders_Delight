package com.zrollus.bd.essentials;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zrollus.bd.Lib.ItemNameCommand;
import com.zrollus.bd.Lib.LocationStorageLib;
import com.zrollus.bd.Lib.ModConfigHelper;
import com.zrollus.bd.Lib.PermissionCompat;
import com.zrollus.bd.Lib.PokedollarHandler;
import com.zrollus.bd.Lib.libHelpers.BDLocation;
import com.zrollus.bd.Lib.libHelpers.GlobalWarpModel;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** EssentialsX-inspired commands implemented against Fabric and LuckPerms. */
public final class EssentialsCommands {
    private EssentialsCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(literal("afk").requires(s -> allowed(s, "command.afk", 0))
                    .executes(EssentialsCommands::afk));
            dispatcher.register(literal("vanish").requires(s -> allowed(s, "command.vanish", 2))
                    .executes(c -> vanish(c, c.getSource().getPlayerOrThrow()))
                    .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.vanish.others", 2))
                            .executes(c -> vanish(c, EntityArgumentType.getPlayer(c, "player")))));
            dispatcher.register(literal("v").redirect(dispatcher.register(literal("bdvanish")
                    .requires(s -> allowed(s, "command.vanish", 2)).executes(c -> vanish(c, c.getSource().getPlayerOrThrow())))));
            dispatcher.register(literal("god").requires(s -> allowed(s, "command.god", 2))
                    .executes(c -> god(c, c.getSource().getPlayerOrThrow()))
                    .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.god.others", 2))
                            .executes(c -> god(c, EntityArgumentType.getPlayer(c, "player")))));
            dispatcher.register(literal("mute").requires(s -> allowed(s, "command.mute", 2))
                    .then(argument("player", EntityArgumentType.player())
                            .executes(c -> mute(c, EntityArgumentType.getPlayer(c, "player"), "perm", "Muted by an operator"))
                            .then(argument("duration", StringArgumentType.word())
                                    .executes(c -> mute(c, EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "duration"), "Muted by an operator"))
                                    .then(argument("reason", StringArgumentType.greedyString())
                                            .executes(c -> mute(c, EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "duration"), StringArgumentType.getString(c, "reason")))))));
            dispatcher.register(literal("unmute").requires(s -> allowed(s, "command.mute", 2))
                    .then(argument("player", EntityArgumentType.player()).executes(EssentialsCommands::unmute)));
            dispatcher.register(literal("ignore").requires(s -> allowed(s, "command.ignore", 0))
                    .then(argument("player", EntityArgumentType.player()).executes(EssentialsCommands::ignore)));
            dispatcher.register(literal("msgtoggle").requires(s -> allowed(s, "command.msgtoggle", 0))
                    .executes(EssentialsCommands::messageToggle));
            dispatcher.register(literal("paytoggle").requires(s -> allowed(s, "command.paytoggle", 0))
                    .executes(EssentialsCommands::payToggle));
            dispatcher.register(literal("socialspy").requires(s -> allowed(s, "command.socialspy", 2))
                    .executes(EssentialsCommands::socialSpy));

            dispatcher.register(literal("heal").requires(s -> allowed(s, "command.heal", 2))
                    .executes(c -> heal(c, c.getSource().getPlayerOrThrow()))
                    .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.heal.others", 2))
                            .executes(c -> heal(c, EntityArgumentType.getPlayer(c, "player")))));
            dispatcher.register(literal("feed").requires(s -> allowed(s, "command.feed", 2))
                    .executes(c -> feed(c, c.getSource().getPlayerOrThrow()))
                    .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.feed.others", 2))
                            .executes(c -> feed(c, EntityArgumentType.getPlayer(c, "player")))));
            dispatcher.register(literal("fly").requires(s -> allowed(s, "command.fly", 2))
                    .executes(c -> fly(c, c.getSource().getPlayerOrThrow()))
                    .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.fly.others", 2))
                            .executes(c -> fly(c, EntityArgumentType.getPlayer(c, "player")))));
            dispatcher.register(literal("speed").requires(s -> allowed(s, "command.speed", 2))
                    .then(argument("speed", DoubleArgumentType.doubleArg(0, 10))
                            .executes(c -> speed(c, c.getSource().getPlayerOrThrow(), DoubleArgumentType.getDouble(c, "speed")))
                            .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.speed.others", 2))
                                    .executes(c -> speed(c, EntityArgumentType.getPlayer(c, "player"), DoubleArgumentType.getDouble(c, "speed"))))));
            dispatcher.register(literal("repair").requires(s -> allowed(s, "command.repair", 2))
                    .executes(EssentialsCommands::repairHand)
                    .then(literal("hand").executes(EssentialsCommands::repairHand))
                    .then(literal("all").requires(s -> allowed(s, "command.repair.all", 2)).executes(EssentialsCommands::repairAll)));
            dispatcher.register(literal("more").requires(s -> allowed(s, "command.more", 2))
                    .executes(c -> more(c, -1))
                    .then(argument("amount", IntegerArgumentType.integer(1, 99)).executes(c -> more(c, IntegerArgumentType.getInteger(c, "amount")))));
            dispatcher.register(literal("hat").requires(s -> allowed(s, "command.hat", 0)).executes(EssentialsCommands::hat));

            registerPortableMenus(dispatcher);
            registerItemLore(dispatcher);
            registerEconomy(dispatcher);
            registerKits(dispatcher);
            registerTeleportUtilities(dispatcher);
            registerJails(dispatcher);
            registerInformation(dispatcher);
        });
    }

    private static void registerPortableMenus(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("workbench").requires(s -> allowed(s, "command.workbench", 0))
                .executes(c -> openMenu(c, "Crafting", (id, inv) -> new CraftingScreenHandler(id, inv))));
        dispatcher.register(literal("anvil").requires(s -> allowed(s, "command.anvil", 0))
                .executes(c -> openMenu(c, "Anvil", (id, inv) -> new AnvilScreenHandler(id, inv, ScreenHandlerContext.EMPTY))));
        dispatcher.register(literal("grindstone").requires(s -> allowed(s, "command.grindstone", 0))
                .executes(c -> openMenu(c, "Grindstone", (id, inv) -> new GrindstoneScreenHandler(id, inv, ScreenHandlerContext.EMPTY))));
        dispatcher.register(literal("stonecutter").requires(s -> allowed(s, "command.stonecutter", 0))
                .executes(c -> openMenu(c, "Stonecutter", (id, inv) -> new StonecutterScreenHandler(id, inv, ScreenHandlerContext.EMPTY))));
        dispatcher.register(literal("loom").requires(s -> allowed(s, "command.loom", 0))
                .executes(c -> openMenu(c, "Loom", (id, inv) -> new LoomScreenHandler(id, inv, ScreenHandlerContext.EMPTY))));
        dispatcher.register(literal("cartographytable").requires(s -> allowed(s, "command.cartographytable", 0))
                .executes(c -> openMenu(c, "Cartography Table", (id, inv) -> new CartographyTableScreenHandler(id, inv, ScreenHandlerContext.EMPTY))));
        dispatcher.register(literal("smithingtable").requires(s -> allowed(s, "command.smithingtable", 0))
                .executes(c -> openMenu(c, "Smithing Table", (id, inv) -> new SmithingScreenHandler(id, inv, ScreenHandlerContext.EMPTY))));
        dispatcher.register(literal("enderchest").requires(s -> allowed(s, "command.enderchest", 0))
                .executes(EssentialsCommands::enderChest));
        dispatcher.register(literal("disposal").requires(s -> allowed(s, "command.disposal", 0))
                .executes(EssentialsCommands::disposal));
        dispatcher.register(literal("trash").requires(s -> allowed(s, "command.disposal", 0))
                .executes(EssentialsCommands::disposal));
    }

    private static void registerItemLore(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("itemlore").requires(s -> allowed(s, "command.itemlore", 0))
                .then(literal("add").then(argument("text", StringArgumentType.greedyString()).executes(c -> loreAdd(c, StringArgumentType.getString(c, "text")))))
                .then(literal("set").then(argument("line", IntegerArgumentType.integer(1, 256))
                        .then(argument("text", StringArgumentType.greedyString()).executes(EssentialsCommands::loreSet))))
                .then(literal("remove").then(argument("line", IntegerArgumentType.integer(1, 256)).executes(EssentialsCommands::loreRemove)))
                .then(literal("clear").executes(EssentialsCommands::loreClear)));
        dispatcher.register(literal("lore").redirect(dispatcher.getRoot().getChild("itemlore")));
    }

    private static void registerEconomy(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("balance").executes(c -> showBalance(c, c.getSource().getPlayerOrThrow()))
                .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.balance.others", 0))
                        .executes(c -> showBalance(c, EntityArgumentType.getPlayer(c, "player")))));
        dispatcher.register(literal("money").redirect(dispatcher.getRoot().getChild("balance")));
        dispatcher.register(literal("eco").requires(s -> allowed(s, "command.eco", 2))
                .then(literal("set").then(argument("player", EntityArgumentType.player()).then(argument("amount", LongArgumentType.longArg(0)).executes(c -> eco(c, "set")))))
                .then(literal("give").then(argument("player", EntityArgumentType.player()).then(argument("amount", LongArgumentType.longArg(1)).executes(c -> eco(c, "give")))))
                .then(literal("take").then(argument("player", EntityArgumentType.player()).then(argument("amount", LongArgumentType.longArg(1)).executes(c -> eco(c, "take"))))));
        dispatcher.register(literal("setworth").requires(s -> allowed(s, "command.setworth", 2))
                .then(argument("value", LongArgumentType.longArg(0)).executes(EssentialsCommands::setWorth)));
        dispatcher.register(literal("worth").requires(s -> allowed(s, "command.worth", 0)).executes(EssentialsCommands::worth));
        dispatcher.register(literal("sell").requires(s -> allowed(s, "command.sell", 0))
                .executes(c -> sell(c, false)).then(literal("inventory").executes(c -> sell(c, true)))
                .then(literal("hand").executes(c -> sell(c, false))));
        dispatcher.register(literal("transactionhistory").requires(s -> allowed(s, "command.transactionhistory", 2))
                .executes(EssentialsCommands::transactionHistory));
    }

    private static void registerKits(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("kit").requires(s -> allowed(s, "command.kit", 0))
                .executes(EssentialsCommands::listKits)
                .then(argument("name", StringArgumentType.word()).suggests((c, b) -> {
                    EssentialsManager.get(c.getSource().getServer()).kits.keySet().forEach(b::suggest); return b.buildFuture();
                }).executes(EssentialsCommands::giveKit)));
        dispatcher.register(literal("kits").redirect(dispatcher.getRoot().getChild("kit")));
        dispatcher.register(literal("createkit").requires(s -> allowed(s, "command.createkit", 2))
                .then(argument("name", StringArgumentType.word()).executes(c -> createKit(c, 0))
                        .then(argument("cooldownSeconds", LongArgumentType.longArg(-1)).executes(c -> createKit(c, LongArgumentType.getLong(c, "cooldownSeconds"))))));
        dispatcher.register(literal("delkit").requires(s -> allowed(s, "command.delkit", 2))
                .then(argument("name", StringArgumentType.word()).executes(EssentialsCommands::deleteKit)));
        dispatcher.register(literal("showkit").requires(s -> allowed(s, "command.showkit", 0))
                .then(argument("name", StringArgumentType.word()).executes(EssentialsCommands::showKit)));
        dispatcher.register(literal("kitreset").requires(s -> allowed(s, "command.kitreset", 2))
                .then(argument("player", EntityArgumentType.player()).executes(EssentialsCommands::kitReset)));
    }

    private static void registerTeleportUtilities(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("top").requires(s -> allowed(s, "command.top", 0)).executes(c -> verticalTeleport(c, true)));
        dispatcher.register(literal("bottom").requires(s -> allowed(s, "command.bottom", 0)).executes(c -> verticalTeleport(c, false)));
        dispatcher.register(literal("jump").requires(s -> allowed(s, "command.jump", 0)).executes(EssentialsCommands::jump));
        dispatcher.register(literal("tppos").requires(s -> allowed(s, "command.tppos", 2))
                .then(argument("x", DoubleArgumentType.doubleArg()).then(argument("y", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg()).executes(EssentialsCommands::tpPos)))));
        dispatcher.register(literal("tpr").requires(s -> allowed(s, "command.tpr", 0)).executes(EssentialsCommands::randomTeleport));
        dispatcher.register(literal("tphere").requires(s -> allowed(s, "command.tphere", 2))
                .then(argument("player", EntityArgumentType.player()).executes(EssentialsCommands::teleportHere)));
        dispatcher.register(literal("tpall").requires(s -> allowed(s, "command.tpall", 2)).executes(EssentialsCommands::teleportAll));
        dispatcher.register(literal("tpaall").requires(s -> allowed(s, "command.tpaall", 2)).executes(EssentialsCommands::requestAll));
        dispatcher.register(literal("tpoffline").requires(s -> allowed(s, "command.tpoffline", 2))
                .then(argument("player", StringArgumentType.word()).executes(EssentialsCommands::teleportOffline)));
        dispatcher.register(literal("tptoggle").requires(s -> allowed(s, "command.tptoggle", 0)).executes(EssentialsCommands::teleportToggle));
        dispatcher.register(literal("tpauto").requires(s -> allowed(s, "command.tpauto", 0)).executes(EssentialsCommands::teleportAuto));
        dispatcher.register(literal("warps").requires(s -> allowed(s, "command.warps", 0)).executes(EssentialsCommands::listWarps));
        dispatcher.register(literal("delwarp").requires(s -> allowed(s, "command.delwarp", 2))
                .then(argument("name", StringArgumentType.word()).executes(EssentialsCommands::deleteWarp)));
        dispatcher.register(literal("warpinfo").requires(s -> allowed(s, "command.warpinfo", 0))
                .then(argument("name", StringArgumentType.word()).executes(EssentialsCommands::warpInfo)));
        dispatcher.register(literal("renamehome").requires(s -> allowed(s, "command.renamehome", 0))
                .then(argument("old", StringArgumentType.word()).then(argument("new", StringArgumentType.word()).executes(EssentialsCommands::renameHome))));
    }

    private static void registerJails(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("setjail").requires(s -> allowed(s, "command.setjail", 2))
                .then(argument("name", StringArgumentType.word()).executes(EssentialsCommands::setJail)));
        dispatcher.register(literal("deljail").requires(s -> allowed(s, "command.deljail", 2))
                .then(argument("name", StringArgumentType.word()).executes(EssentialsCommands::deleteJail)));
        dispatcher.register(literal("jails").requires(s -> allowed(s, "command.jails", 0)).executes(EssentialsCommands::listJails));
        var jailDuration = argument("duration", StringArgumentType.word())
                .executes(c -> jail(c, StringArgumentType.getString(c, "duration"), "Jailed by an operator"))
                .then(argument("reason", StringArgumentType.greedyString())
                        .executes(c -> jail(c, StringArgumentType.getString(c, "duration"), StringArgumentType.getString(c, "reason"))));
        dispatcher.register(literal("jail").requires(s -> allowed(s, "command.jail", 2))
                .then(argument("player", EntityArgumentType.player())
                        .then(argument("jail", StringArgumentType.word())
                                .executes(c -> jail(c, "perm", "Jailed by an operator"))
                                .then(jailDuration))));
        dispatcher.register(literal("unjail").requires(s -> allowed(s, "command.jail", 2))
                .then(argument("player", EntityArgumentType.player()).executes(EssentialsCommands::unJail)));
    }

    private static void registerInformation(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("near").requires(s -> allowed(s, "command.near", 0))
                .executes(c -> near(c, 200)).then(argument("radius", IntegerArgumentType.integer(1, 10000)).executes(c -> near(c, IntegerArgumentType.getInteger(c, "radius")))));
        dispatcher.register(literal("playtime").requires(s -> allowed(s, "command.playtime", 0))
                .executes(c -> playtime(c, c.getSource().getPlayerOrThrow()))
                .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.playtime.others", 0)).executes(c -> playtime(c, EntityArgumentType.getPlayer(c, "player")))));
        dispatcher.register(literal("ping").requires(s -> allowed(s, "command.ping", 0)).executes(EssentialsCommands::ping));
        dispatcher.register(literal("whois").requires(s -> allowed(s, "command.whois", 2))
                .then(argument("player", EntityArgumentType.player()).executes(EssentialsCommands::whois)));
        dispatcher.register(literal("realname").requires(s -> allowed(s, "command.realname", 0))
                .then(argument("name", StringArgumentType.word()).executes(EssentialsCommands::realName)));
        dispatcher.register(literal("helpop").requires(s -> allowed(s, "command.helpop", 0))
                .then(argument("message", MessageArgumentType.message()).executes(EssentialsCommands::helpOp)));
        dispatcher.register(literal("broadcast").requires(s -> allowed(s, "command.broadcast", 2))
                .then(argument("message", MessageArgumentType.message()).executes(EssentialsCommands::broadcast)));
        dispatcher.register(literal("broadcastworld").requires(s -> allowed(s, "command.broadcastworld", 2))
                .then(argument("message", MessageArgumentType.message()).executes(EssentialsCommands::broadcastWorld)));
        dispatcher.register(literal("me").requires(s -> allowed(s, "command.me", 0))
                .then(argument("message", MessageArgumentType.message()).executes(EssentialsCommands::me)));
        dispatcher.register(literal("ext").requires(s -> allowed(s, "command.ext", 2))
                .executes(c -> extinguish(c, c.getSource().getPlayerOrThrow()))
                .then(argument("player", EntityArgumentType.player()).requires(s -> allowed(s, "command.ext.others", 2)).executes(c -> extinguish(c, EntityArgumentType.getPlayer(c, "player")))));
        dispatcher.register(literal("burn").requires(s -> allowed(s, "command.burn", 2))
                .then(argument("player", EntityArgumentType.player()).then(argument("seconds", IntegerArgumentType.integer(1, 3600)).executes(EssentialsCommands::burn))));
        dispatcher.register(literal("suicide").requires(s -> allowed(s, "command.suicide", 0)).executes(EssentialsCommands::suicide));
        dispatcher.register(literal("exp").requires(s -> allowed(s, "command.exp", 0))
                .executes(c -> experience(c, c.getSource().getPlayerOrThrow(), "show", 0))
                .then(literal("show").executes(c -> experience(c, c.getSource().getPlayerOrThrow(), "show", 0)))
                .then(literal("reset").executes(c -> experience(c, c.getSource().getPlayerOrThrow(), "set", 0)))
                .then(literal("set").requires(s -> allowed(s, "command.exp.set", 2)).then(argument("amount", IntegerArgumentType.integer(0)).executes(c -> experience(c, c.getSource().getPlayerOrThrow(), "set", IntegerArgumentType.getInteger(c, "amount")))))
                .then(literal("give").requires(s -> allowed(s, "command.exp.give", 2)).then(argument("amount", IntegerArgumentType.integer()).executes(c -> experience(c, c.getSource().getPlayerOrThrow(), "give", IntegerArgumentType.getInteger(c, "amount"))))));
        dispatcher.register(literal("gc").requires(s -> allowed(s, "command.gc", 2)).executes(EssentialsCommands::gc));
        dispatcher.register(literal("ptime").requires(s -> allowed(s, "command.ptime", 0))
                .then(argument("time", StringArgumentType.word()).executes(EssentialsCommands::playerTime)));
        dispatcher.register(literal("pweather").requires(s -> allowed(s, "command.pweather", 0))
                .then(argument("weather", StringArgumentType.word()).executes(EssentialsCommands::playerWeather)));
        dispatcher.register(literal("motd").requires(s -> allowed(s, "command.motd", 0)).executes(c -> info(c, ModConfigHelper.get().motd)));
        dispatcher.register(literal("rules").requires(s -> allowed(s, "command.rules", 0)).executes(c -> info(c, ModConfigHelper.get().rules)));
        dispatcher.register(literal("info").requires(s -> allowed(s, "command.info", 0)).executes(c -> info(c, ModConfigHelper.get().serverInfo)));
    }

    private static boolean allowed(ServerCommandSource source, String node, int fallback) {
        return PermissionCompat.check(source, node, fallback);
    }

    private static int afk(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow();
        boolean current = EssentialsManager.get(c.getSource().getServer()).player(player.getUuid()).afk;
        EssentialsSystem.setAfk(player, !current, true); return 1;
    }

    private static int vanish(CommandContext<ServerCommandSource> c, ServerPlayerEntity player) {
        EssentialsManager manager = EssentialsManager.get(c.getSource().getServer());
        EssentialsManager.PlayerState state = manager.player(player.getUuid());
        state.vanished = !state.vanished;
        player.setInvisible(state.vanished);
        if (state.vanished) player.addCommandTag("bd_vanished"); else player.removeCommandTag("bd_vanished");
        EssentialsSystem.syncVanishVisibility(player, state.vanished);
        manager.changed();
        player.sendMessage(Text.literal("Vanish " + (state.vanished ? "enabled" : "disabled") + ".")
                .formatted(state.vanished ? Formatting.GREEN : Formatting.YELLOW), false);
        return 1;
    }

    private static int god(CommandContext<ServerCommandSource> c, ServerPlayerEntity player) {
        EssentialsManager manager = EssentialsManager.get(c.getSource().getServer());
        EssentialsManager.PlayerState state = manager.player(player.getUuid());
        state.god = !state.god;
        if (state.god) player.addCommandTag("bd_god"); else player.removeCommandTag("bd_god");
        manager.changed(); return success(c, "God mode " + (state.god ? "enabled" : "disabled") + " for " + player.getName().getString() + ".");
    }

    private static int mute(CommandContext<ServerCommandSource> c, ServerPlayerEntity player, String duration, String reason) {
        long millis = parseDuration(duration);
        if (millis == Long.MIN_VALUE) return error(c, "Use a duration such as 30s, 10m, 2h, 7d, or perm.");
        EssentialsManager manager = EssentialsManager.get(c.getSource().getServer());
        EssentialsManager.PlayerState state = manager.player(player.getUuid());
        state.mutedUntil = millis < 0 ? -1 : System.currentTimeMillis() + millis;
        state.muteReason = reason;
        manager.changed();
        player.sendMessage(Text.literal("You were muted: " + reason).formatted(Formatting.RED));
        return success(c, "Muted " + player.getName().getString() + ".");
    }

    private static int unmute(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(c, "player");
        EssentialsManager.PlayerState state = EssentialsManager.get(c.getSource().getServer()).player(player.getUuid());
        state.mutedUntil = 0; state.muteReason = ""; EssentialsManager.get(c.getSource().getServer()).changed();
        return success(c, "Unmuted " + player.getName().getString() + ".");
    }

    private static int ignore(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity self = c.getSource().getPlayerOrThrow(), target = EntityArgumentType.getPlayer(c, "player");
        if (self == target) return error(c, "You cannot ignore yourself.");
        EssentialsManager manager = EssentialsManager.get(c.getSource().getServer());
        boolean added = manager.player(self.getUuid()).ignored.add(target.getUuid());
        if (!added) manager.player(self.getUuid()).ignored.remove(target.getUuid());
        manager.changed(); return success(c, (added ? "Ignoring " : "No longer ignoring ") + target.getName().getString() + ".");
    }

    private static int messageToggle(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        EssentialsManager manager = EssentialsManager.get(c.getSource().getServer());
        EssentialsManager.PlayerState state = manager.player(c.getSource().getPlayerOrThrow().getUuid());
        state.messagesEnabled = !state.messagesEnabled; manager.changed();
        return success(c, "Private messages " + (state.messagesEnabled ? "enabled" : "disabled") + ".");
    }

    private static int payToggle(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        EssentialsManager manager = EssentialsManager.get(c.getSource().getServer());
        EssentialsManager.PlayerState state = manager.player(c.getSource().getPlayerOrThrow().getUuid());
        state.paymentsEnabled = !state.paymentsEnabled; manager.changed();
        return success(c, "Incoming payments " + (state.paymentsEnabled ? "enabled" : "disabled") + ".");
    }

    private static int socialSpy(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        EssentialsManager manager = EssentialsManager.get(c.getSource().getServer());
        EssentialsManager.PlayerState state = manager.player(c.getSource().getPlayerOrThrow().getUuid());
        state.socialSpy = !state.socialSpy; manager.changed(); return success(c, "SocialSpy " + (state.socialSpy ? "enabled" : "disabled") + ".");
    }

    private static int heal(CommandContext<ServerCommandSource> c, ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth()); player.clearStatusEffects(); return success(c, "Healed " + player.getName().getString() + ".");
    }
    private static int feed(CommandContext<ServerCommandSource> c, ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(20); player.getHungerManager().setSaturationLevel(20); return success(c, "Fed " + player.getName().getString() + ".");
    }
    private static int fly(CommandContext<ServerCommandSource> c, ServerPlayerEntity player) {
        player.getAbilities().allowFlying = !player.getAbilities().allowFlying;
        if (!player.getAbilities().allowFlying) player.getAbilities().flying = false;
        player.sendAbilitiesUpdate(); return success(c, "Flight " + (player.getAbilities().allowFlying ? "enabled" : "disabled") + " for " + player.getName().getString() + ".");
    }
    private static int speed(CommandContext<ServerCommandSource> c, ServerPlayerEntity player, double value) {
        boolean flying = player.getAbilities().flying;
        float scaled = (float) Math.min(.8, value / 10.0 * .8);
        if (flying) player.getAbilities().setFlySpeed(scaled); else player.getAbilities().setWalkSpeed(scaled);
        player.sendAbilitiesUpdate(); return success(c, (flying ? "Fly" : "Walk") + " speed set to " + value + ".");
    }

    private static int repairHand(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ItemStack stack = c.getSource().getPlayerOrThrow().getMainHandStack();
        if (stack.isEmpty() || !stack.isDamageable()) return error(c, "Hold a damaged item in your main hand.");
        stack.setDamage(0); return success(c, "Item repaired.");
    }
    private static int repairAll(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        int count = 0; ServerPlayerEntity player = c.getSource().getPlayerOrThrow();
        for (int i = 0; i < player.getInventory().size(); i++) { ItemStack stack = player.getInventory().getStack(i); if (stack.isDamageable() && stack.isDamaged()) { stack.setDamage(0); count++; } }
        return success(c, "Repaired " + count + " item(s).");
    }
    private static int more(CommandContext<ServerCommandSource> c, int amount) throws CommandSyntaxException {
        ItemStack stack = c.getSource().getPlayerOrThrow().getMainHandStack(); if (stack.isEmpty()) return error(c, "Hold an item first.");
        stack.setCount(amount < 1 ? stack.getMaxCount() : Math.min(amount, stack.getMaxCount())); return success(c, "Stack filled to " + stack.getCount() + ".");
    }
    private static int hat(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); ItemStack held = player.getMainHandStack().copy(), old = player.getEquippedStack(EquipmentSlot.HEAD).copy();
        if (held.isEmpty()) return error(c, "Hold the item you want to wear.");
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, old); player.equipStack(EquipmentSlot.HEAD, held); return success(c, "Enjoy your new hat.");
    }

    @FunctionalInterface private interface MenuFactory { net.minecraft.screen.ScreenHandler create(int id, PlayerInventory inventory); }
    private static int openMenu(CommandContext<ServerCommandSource> c, String title, MenuFactory factory) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow();
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id, inv, ignored) -> factory.create(id, inv), Text.literal(title))); return 1;
    }
    private static int enderChest(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow();
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id, inv, ignored) -> GenericContainerScreenHandler.createGeneric9x3(id, inv, player.getEnderChestInventory()), Text.literal("Ender Chest"))); return 1;
    }
    private static int disposal(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); SimpleInventory trash = new SimpleInventory(54);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id, inv, ignored) -> GenericContainerScreenHandler.createGeneric9x6(id, inv, trash), Text.literal("Disposal"))); return 1;
    }

    private static ItemStack held(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { return c.getSource().getPlayerOrThrow().getMainHandStack(); }
    private static int loreAdd(CommandContext<ServerCommandSource> c, String value) throws CommandSyntaxException {
        ItemStack stack = held(c); if (stack.isEmpty()) return error(c, "Hold an item first."); LoreComponent lore = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
        stack.set(DataComponentTypes.LORE, lore.with(ItemNameCommand.parseLegacyFormatting(value))); return success(c, "Lore line added.");
    }
    private static int loreSet(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ItemStack stack = held(c); if (stack.isEmpty()) return error(c, "Hold an item first."); int line = IntegerArgumentType.getInteger(c, "line") - 1;
        List<Text> lines = new ArrayList<>(stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines());
        while (lines.size() <= line) lines.add(Text.empty()); lines.set(line, ItemNameCommand.parseLegacyFormatting(StringArgumentType.getString(c, "text")));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lines)); return success(c, "Lore line updated.");
    }
    private static int loreRemove(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ItemStack stack = held(c); int line = IntegerArgumentType.getInteger(c, "line") - 1; List<Text> lines = new ArrayList<>(stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines());
        if (line < 0 || line >= lines.size()) return error(c, "That lore line does not exist."); lines.remove(line); stack.set(DataComponentTypes.LORE, new LoreComponent(lines)); return success(c, "Lore line removed.");
    }
    private static int loreClear(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ItemStack stack = held(c); stack.remove(DataComponentTypes.LORE); return success(c, "Lore cleared."); }

    private static int showBalance(CommandContext<ServerCommandSource> c, ServerPlayerEntity player) {
        PokedollarHandler.syncPhysicalToVirtual(player); PlayerDataModel data = LocationStorageLib.getPlayerData(c.getSource().getServer(), player.getUuid());
        c.getSource().sendMessage(Text.literal(player.getName().getString() + " has ₱" + data.bal).formatted(Formatting.GOLD)); return 1;
    }
    private static int eco(CommandContext<ServerCommandSource> c, String operation) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(c, "player"); long amount = LongArgumentType.getLong(c, "amount");
        PlayerDataModel data = LocationStorageLib.getPlayerData(c.getSource().getServer(), player.getUuid());
        data.bal = switch (operation) { case "set" -> amount; case "give" -> Math.addExact(data.bal, amount); default -> Math.max(0, data.bal - amount); };
        LocationStorageLib.savePlayerData(c.getSource().getServer(), player.getUuid(), data);
        EssentialsManager.get(c.getSource().getServer()).logTransaction(operation + " ₱" + amount + " " + player.getName().getString() + " by " + c.getSource().getName());
        return success(c, "Balance updated to ₱" + data.bal + ".");
    }
    private static String heldId(ServerPlayerEntity player) { return Registries.ITEM.getId(player.getMainHandStack().getItem()).toString(); }
    private static int setWorth(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); if (player.getMainHandStack().isEmpty()) return error(c, "Hold an item first.");
        long value = LongArgumentType.getLong(c, "value"); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); manager.worth.put(heldId(player), value); manager.changed(); return success(c, "Worth set to ₱" + value + " per item.");
    }
    private static int worth(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); ItemStack stack = player.getMainHandStack(); if (stack.isEmpty()) return error(c, "Hold an item first.");
        Long value = EssentialsManager.get(c.getSource().getServer()).worth.get(heldId(player)); if (value == null) return error(c, "No worth is configured for this item.");
        return success(c, stack.getCount() + " item(s) are worth ₱" + Math.multiplyExact(value, stack.getCount()) + ".");
    }
    private static int sell(CommandContext<ServerCommandSource> c, boolean inventory) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); long total = 0; int sold = 0;
        int start = inventory ? 0 : player.getInventory().selectedSlot, end = inventory ? player.getInventory().main.size() : start + 1;
        for (int i = start; i < end; i++) { ItemStack stack = player.getInventory().getStack(i); if (stack.isEmpty()) continue; Long price = manager.worth.get(Registries.ITEM.getId(stack.getItem()).toString()); if (price == null) continue; total = Math.addExact(total, Math.multiplyExact(price, stack.getCount())); sold += stack.getCount(); stack.setCount(0); }
        if (sold == 0) return error(c, "None of those items have a configured worth."); PlayerDataModel data = LocationStorageLib.getPlayerData(c.getSource().getServer(), player.getUuid()); data.bal = Math.addExact(data.bal, total); LocationStorageLib.savePlayerData(c.getSource().getServer(), player.getUuid(), data); manager.logTransaction("sell ₱" + total + " " + player.getName().getString()); return success(c, "Sold " + sold + " item(s) for ₱" + total + ".");
    }
    private static int transactionHistory(CommandContext<ServerCommandSource> c) { EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); c.getSource().sendMessage(Text.literal("Recent economy transactions").formatted(Formatting.GOLD, Formatting.BOLD)); manager.transactions.stream().limit(20).forEach(line -> c.getSource().sendMessage(Text.literal(line).formatted(Formatting.GRAY))); return 1; }

    private static int createKit(CommandContext<ServerCommandSource> c, long cooldown) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT); EssentialsManager.Kit kit = new EssentialsManager.Kit(); kit.cooldownSeconds = cooldown;
        for (int i = 0; i < player.getInventory().size(); i++) { ItemStack stack = player.getInventory().getStack(i); if (!stack.isEmpty()) kit.contents.add(stack.copy()); }
        if (kit.contents.isEmpty()) return error(c, "Your inventory is empty."); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); manager.kits.put(name, kit); manager.changed(); return success(c, "Kit " + name + " saved with " + kit.contents.size() + " stack(s).");
    }
    private static int giveKit(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); EssentialsManager.Kit kit = manager.kits.get(name); if (kit == null) return error(c, "Unknown kit.");
        EssentialsManager.PlayerState state = manager.player(player.getUuid()); long last = state.kitUses.getOrDefault(name, 0L), now = System.currentTimeMillis(); if (kit.cooldownSeconds < 0 && last > 0) return error(c, "That one-time kit has already been used."); if (kit.cooldownSeconds > 0 && now - last < kit.cooldownSeconds * 1000L) return error(c, "Kit available again in " + ((kit.cooldownSeconds * 1000L - (now - last) + 999) / 1000) + " seconds.");
        for (ItemStack stack : kit.contents) player.getInventory().offerOrDrop(stack.copy()); state.kitUses.put(name, now); manager.changed(); return success(c, "Kit " + name + " received.");
    }
    private static int listKits(CommandContext<ServerCommandSource> c) { return info(c, "Kits: " + String.join(", ", EssentialsManager.get(c.getSource().getServer()).kits.keySet())); }
    private static int deleteKit(CommandContext<ServerCommandSource> c) { String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); if (manager.kits.remove(name) == null) return error(c, "Unknown kit."); manager.changed(); return success(c, "Kit deleted."); }
    private static int showKit(CommandContext<ServerCommandSource> c) { String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT); EssentialsManager.Kit kit = EssentialsManager.get(c.getSource().getServer()).kits.get(name); if (kit == null) return error(c, "Unknown kit."); c.getSource().sendMessage(Text.literal("Kit " + name + " (cooldown " + kit.cooldownSeconds + "s)").formatted(Formatting.GOLD)); kit.contents.forEach(stack -> c.getSource().sendMessage(Text.literal(stack.getCount() + "x " + stack.getName().getString()).formatted(Formatting.GRAY))); return 1; }
    private static int kitReset(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity player = EntityArgumentType.getPlayer(c, "player"); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); manager.player(player.getUuid()).kitUses.clear(); manager.changed(); return success(c, "Kit cooldowns reset."); }

    private static int verticalTeleport(CommandContext<ServerCommandSource> c, boolean top) throws CommandSyntaxException { ServerPlayerEntity p = c.getSource().getPlayerOrThrow(); ServerWorld w = p.getServerWorld(); int x = p.getBlockX(), z = p.getBlockZ(); int y = top ? w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) : w.getBottomY() + 1; if (!top) { for (; y < w.getTopY(); y++) if (!w.getBlockState(new BlockPos(x, y, z)).isAir()) { y++; break; } } p.teleport(w, x + .5, y, z + .5, p.getYaw(), p.getPitch()); return success(c, "Teleported " + (top ? "to the top." : "to the bottom.")); }
    private static int jump(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = c.getSource().getPlayerOrThrow(); var hit = p.raycast(120, 0, false); if (hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) return error(c, "No block in sight."); BlockPos pos = ((net.minecraft.util.hit.BlockHitResult) hit).getBlockPos().offset(((net.minecraft.util.hit.BlockHitResult) hit).getSide()); p.teleport(p.getServerWorld(), pos.getX() + .5, pos.getY(), pos.getZ() + .5, p.getYaw(), p.getPitch()); return success(c, "Jumped to the target block."); }
    private static int tpPos(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = c.getSource().getPlayerOrThrow(); p.teleport(p.getServerWorld(), DoubleArgumentType.getDouble(c, "x"), DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z"), p.getYaw(), p.getPitch()); return success(c, "Teleported."); }
    private static int randomTeleport(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = c.getSource().getPlayerOrThrow(); ServerWorld w = p.getServerWorld(); for (int tries = 0; tries < 64; tries++) { int x = w.random.nextBetween(-5000, 5000), z = w.random.nextBetween(-5000, 5000), y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z); BlockPos floor = new BlockPos(x, y - 1, z); if (!w.getFluidState(floor).isEmpty()) continue; p.teleport(w, x + .5, y, z + .5, p.getYaw(), p.getPitch()); return success(c, "Randomly teleported to " + x + ", " + z + "."); } return error(c, "Could not find a safe random location."); }
    private static int teleportHere(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity self = c.getSource().getPlayerOrThrow(), target = EntityArgumentType.getPlayer(c, "player"); TeleportService.teleport(target, self.getServerWorld(), self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch(), true, true); return success(c, "Teleported " + target.getName().getString() + " to you."); }
    private static int teleportAll(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity self = c.getSource().getPlayerOrThrow(); int count = 0; for (ServerPlayerEntity target : c.getSource().getServer().getPlayerManager().getPlayerList()) if (target != self) { TeleportService.teleport(target, self.getServerWorld(), self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch(), true, true); count++; } return success(c, "Teleported " + count + " player(s) to you."); }
    private static int requestAll(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity self = c.getSource().getPlayerOrThrow(); int count = 0; for (ServerPlayerEntity target : c.getSource().getServer().getPlayerManager().getPlayerList()) if (target != self) { c.getSource().getServer().getCommandManager().executeWithPrefix(self.getCommandSource(), "tpahere " + target.getName().getString()); count++; } return success(c, "Sent " + count + " teleport request(s)."); }
    private static int teleportOffline(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity self = c.getSource().getPlayerOrThrow(); String name = StringArgumentType.getString(c, "player"); var profile = c.getSource().getServer().getUserCache().findByName(name); if (profile.isEmpty()) return error(c, "Unknown player."); PlayerDataModel data = LocationStorageLib.getPlayerData(c.getSource().getServer(), profile.get().getId()); if (data.coords == null || data.worldId == null) return error(c, "No logout location is saved for that player."); ServerWorld world = LocationStorageLib.getWorldFromString(self, data.worldId); TeleportService.teleport(self, world, data.coords.x, data.coords.y, data.coords.z, data.lastYaw, data.lastPitch, true, true); return success(c, "Teleporting to " + profile.get().getName() + "'s logout location."); }
    private static int teleportToggle(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); EssentialsManager.PlayerState state = manager.player(player.getUuid()); state.teleportEnabled = !state.teleportEnabled; manager.changed(); PlayerDataModel data = LocationStorageLib.getPlayerData(c.getSource().getServer(), player.getUuid()); data.tpaEnabled = state.teleportEnabled; LocationStorageLib.savePlayerData(c.getSource().getServer(), player.getUuid(), data); return success(c, "Incoming teleports " + (state.teleportEnabled ? "enabled" : "disabled") + "."); }
    private static int teleportAuto(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); EssentialsManager.PlayerState state = manager.player(c.getSource().getPlayerOrThrow().getUuid()); state.autoTeleport = !state.autoTeleport; manager.changed(); return success(c, "Automatic TPA acceptance " + (state.autoTeleport ? "enabled" : "disabled") + "."); }
    private static int listWarps(CommandContext<ServerCommandSource> c) { return info(c, "Warps: " + String.join(", ", LocationStorageLib.getGlobalWarps(c.getSource().getServer()).warps.keySet())); }
    private static int deleteWarp(CommandContext<ServerCommandSource> c) { String name = StringArgumentType.getString(c, "name"); GlobalWarpModel model = LocationStorageLib.getGlobalWarps(c.getSource().getServer()); if (model.warps.remove(name) == null) return error(c, "Unknown warp."); LocationStorageLib.saveGlobalWarps(c.getSource().getServer(), model); return success(c, "Warp deleted."); }
    private static int warpInfo(CommandContext<ServerCommandSource> c) { String name = StringArgumentType.getString(c, "name"); BDLocation loc = LocationStorageLib.getGlobalWarps(c.getSource().getServer()).warps.get(name); if (loc == null) return error(c, "Unknown warp."); return info(c, name + ": " + loc.worldId + " " + Math.round(loc.x) + ", " + Math.round(loc.y) + ", " + Math.round(loc.z)); }
    private static int renameHome(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = c.getSource().getPlayerOrThrow(); String old = StringArgumentType.getString(c, "old"), name = StringArgumentType.getString(c, "new"); PlayerDataModel data = LocationStorageLib.getPlayerData(c.getSource().getServer(), p.getUuid()); BDLocation loc = data.homes.remove(old); if (loc == null) return error(c, "Unknown home."); if (data.homes.containsKey(name)) { data.homes.put(old, loc); return error(c, "A home with the new name already exists."); } data.homes.put(name, loc); LocationStorageLib.savePlayerData(c.getSource().getServer(), p.getUuid(), data); return success(c, "Home renamed."); }

    private static int setJail(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = c.getSource().getPlayerOrThrow(); String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); manager.jails.put(name, new EssentialsManager.SavedLocation(p.getWorld().getRegistryKey().getValue().toString(), p.getBlockPos(), p.getYaw(), p.getPitch())); manager.changed(); return success(c, "Jail " + name + " created."); }
    private static int deleteJail(CommandContext<ServerCommandSource> c) { String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); if (manager.jails.remove(name) == null) return error(c, "Unknown jail."); manager.changed(); return success(c, "Jail deleted."); }
    private static int listJails(CommandContext<ServerCommandSource> c) { return info(c, "Jails: " + String.join(", ", EssentialsManager.get(c.getSource().getServer()).jails.keySet())); }
    private static int jail(CommandContext<ServerCommandSource> c, String duration, String reason) throws CommandSyntaxException { ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "player"); String jail = StringArgumentType.getString(c, "jail").toLowerCase(Locale.ROOT); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); EssentialsManager.SavedLocation loc = manager.jails.get(jail); if (loc == null) return error(c, "Unknown jail."); long millis = parseDuration(duration); if (millis == Long.MIN_VALUE) return error(c, "Invalid duration."); manager.jailed.put(p.getUuid(), new EssentialsManager.JailRecord(jail, millis < 0 ? -1 : System.currentTimeMillis() + millis, reason)); manager.changed(); teleportSaved(c.getSource().getServer(), p, loc); p.sendMessage(Text.literal("You were jailed: " + reason).formatted(Formatting.RED)); return success(c, "Jailed " + p.getName().getString() + "."); }
    private static int unJail(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "player"); EssentialsManager manager = EssentialsManager.get(c.getSource().getServer()); if (manager.jailed.remove(p.getUuid()) == null) return error(c, "That player is not jailed."); manager.changed(); return success(c, "Released " + p.getName().getString() + "."); }
    private static void teleportSaved(MinecraftServer server, ServerPlayerEntity player, EssentialsManager.SavedLocation loc) { Identifier id = Identifier.tryParse(loc.world()); if (id == null) return; ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id)); if (world != null) player.teleport(world, loc.pos().getX() + .5, loc.pos().getY(), loc.pos().getZ() + .5, loc.yaw(), loc.pitch()); }

    private static int near(CommandContext<ServerCommandSource> c, int radius) throws CommandSyntaxException { ServerPlayerEntity self = c.getSource().getPlayerOrThrow(); MutableText text = Text.literal("Nearby: ").formatted(Formatting.GOLD); boolean found = false; for (ServerPlayerEntity p : c.getSource().getServer().getPlayerManager().getPlayerList()) if (p != self && p.getWorld() == self.getWorld() && p.squaredDistanceTo(self) <= radius * radius && !EssentialsManager.get(c.getSource().getServer()).player(p.getUuid()).vanished) { if (found) text.append(", "); text.append(Text.literal(p.getName().getString()).formatted(Formatting.YELLOW)); found = true; } if (!found) text.append("nobody"); c.getSource().sendMessage(text); return 1; }
    private static int playtime(CommandContext<ServerCommandSource> c, ServerPlayerEntity p) { int ticks = p.getStatHandler().getStat(net.minecraft.stat.Stats.CUSTOM.getOrCreateStat(net.minecraft.stat.Stats.PLAY_TIME)); Duration d = Duration.ofSeconds(ticks / 20L); return info(c, p.getName().getString() + " has played " + d.toDays() + "d " + d.toHoursPart() + "h " + d.toMinutesPart() + "m."); }
    private static int ping(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = c.getSource().getPlayerOrThrow(); return info(c, "Pong! " + p.networkHandler.getLatency() + " ms"); }
    private static int whois(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "player"); EssentialsManager.PlayerState s = EssentialsManager.get(c.getSource().getServer()).player(p.getUuid()); return info(c, p.getName().getString() + " | UUID " + p.getUuid() + " | " + p.getWorld().getRegistryKey().getValue() + " | " + p.getBlockX() + "," + p.getBlockY() + "," + p.getBlockZ() + " | AFK " + s.afk + " | vanished " + s.vanished + " | ping " + p.networkHandler.getLatency() + "ms"); }
    private static int realName(CommandContext<ServerCommandSource> c) { String query = StringArgumentType.getString(c, "name"); for (ServerPlayerEntity player : c.getSource().getServer().getPlayerManager().getPlayerList()) if (player.getDisplayName().getString().equalsIgnoreCase(query) || player.getName().getString().equalsIgnoreCase(query)) return info(c, query + " is " + player.getName().getString() + "."); return error(c, "No online player matches that name."); }
    private static int helpOp(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity sender = c.getSource().getPlayerOrThrow(); Text body = ItemNameCommand.parseLegacyFormatting(MessageArgumentType.getMessage(c, "message").getString()); int sent = 0; for (ServerPlayerEntity viewer : c.getSource().getServer().getPlayerManager().getPlayerList()) if (PermissionCompat.check(viewer, "command.helpop.receive", viewer.hasPermissionLevel(2))) { viewer.sendMessage(Text.literal("[HelpOp] ").formatted(Formatting.RED, Formatting.BOLD).append(Text.literal(sender.getName().getString() + ": ").formatted(Formatting.GOLD)).append(body.copy()), false); sent++; } return success(c, "Message sent to " + sent + " staff member(s)."); }
    private static int broadcast(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { Text message = Text.literal("[Broadcast] ").formatted(Formatting.GOLD, Formatting.BOLD).append(ItemNameCommand.parseLegacyFormatting(MessageArgumentType.getMessage(c, "message").getString())); c.getSource().getServer().getPlayerManager().broadcast(message, false); return 1; }
    private static int broadcastWorld(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity sender = c.getSource().getPlayerOrThrow(); Text message = Text.literal("[World] ").formatted(Formatting.GOLD, Formatting.BOLD).append(ItemNameCommand.parseLegacyFormatting(MessageArgumentType.getMessage(c, "message").getString())); for (ServerPlayerEntity player : c.getSource().getServer().getPlayerManager().getPlayerList()) if (player.getWorld() == sender.getWorld()) player.sendMessage(message, false); return 1; }
    private static int me(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity sender = c.getSource().getPlayerOrThrow(); Text message = Text.literal("* " + sender.getDisplayName().getString() + " ").formatted(Formatting.LIGHT_PURPLE).append(ItemNameCommand.parseLegacyFormatting(MessageArgumentType.getMessage(c, "message").getString())); c.getSource().getServer().getPlayerManager().broadcast(message, false); return 1; }
    private static int extinguish(CommandContext<ServerCommandSource> c, ServerPlayerEntity player) { player.extinguish(); return success(c, "Extinguished " + player.getName().getString() + "."); }
    private static int burn(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity player = EntityArgumentType.getPlayer(c, "player"); player.setOnFireFor(IntegerArgumentType.getInteger(c, "seconds")); return success(c, "Set " + player.getName().getString() + " on fire."); }
    private static int suicide(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); player.damage(player.getDamageSources().genericKill(), Float.MAX_VALUE); return 1; }
    private static int experience(CommandContext<ServerCommandSource> c, ServerPlayerEntity player, String operation, int amount) { if (operation.equals("set")) { player.setExperienceLevel(0); player.setExperiencePoints(0); player.addExperience(amount); } else if (operation.equals("give")) player.addExperience(amount); return info(c, player.getName().getString() + " has " + player.totalExperience + " total experience."); }
    private static int gc(CommandContext<ServerCommandSource> c) { Runtime runtime = Runtime.getRuntime(); long used = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576; long max = runtime.maxMemory() / 1_048_576; return info(c, "Memory: " + used + " / " + max + " MiB | Players: " + c.getSource().getServer().getCurrentPlayerCount() + " | Worlds: " + c.getSource().getServer().getWorlds().spliterator().getExactSizeIfKnown()); }
    private static int playerTime(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); String value = StringArgumentType.getString(c, "time").toLowerCase(Locale.ROOT); long time = switch (value) { case "day" -> 1000; case "night" -> 13000; case "dawn" -> 0; case "reset" -> player.getWorld().getTimeOfDay(); default -> { try { yield Long.parseLong(value); } catch (NumberFormatException ignored) { yield Long.MIN_VALUE; } } }; if (time == Long.MIN_VALUE) return error(c, "Use day, night, dawn, reset, or a tick value."); player.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(player.getWorld().getTime(), time, false)); return success(c, "Personal time updated."); }
    private static int playerWeather(CommandContext<ServerCommandSource> c) throws CommandSyntaxException { ServerPlayerEntity player = c.getSource().getPlayerOrThrow(); String value = StringArgumentType.getString(c, "weather").toLowerCase(Locale.ROOT); if (value.equals("sun") || value.equals("clear")) { player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, 0)); player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 0)); } else if (value.equals("storm") || value.equals("rain")) { player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0)); player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1)); } else if (value.equals("reset")) { boolean raining = player.getWorld().isRaining(); player.networkHandler.sendPacket(new GameStateChangeS2CPacket(raining ? GameStateChangeS2CPacket.RAIN_STARTED : GameStateChangeS2CPacket.RAIN_STOPPED, 0)); } else return error(c, "Use sun, storm, or reset."); return success(c, "Personal weather updated."); }
    private static int info(CommandContext<ServerCommandSource> c, String message) { c.getSource().sendMessage(ItemNameCommand.parseLegacyFormatting(message)); return 1; }

    private static long parseDuration(String value) { if (value == null || value.equalsIgnoreCase("perm") || value.equalsIgnoreCase("permanent")) return -1; try { long multiplier = switch (Character.toLowerCase(value.charAt(value.length() - 1))) { case 's' -> 1000L; case 'm' -> 60_000L; case 'h' -> 3_600_000L; case 'd' -> 86_400_000L; case 'w' -> 604_800_000L; default -> 1L; }; String number = multiplier == 1 ? value : value.substring(0, value.length() - 1); return Math.multiplyExact(Long.parseLong(number), multiplier); } catch (RuntimeException ignored) { return Long.MIN_VALUE; } }
    private static int success(CommandContext<ServerCommandSource> c, String message) { c.getSource().sendMessage(Text.literal(message).formatted(Formatting.GREEN)); return 1; }
    private static int error(CommandContext<ServerCommandSource> c, String message) { c.getSource().sendError(Text.literal(message)); return 0; }
}
