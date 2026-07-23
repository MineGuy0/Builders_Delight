package com.zrollus.bd.shopkeeper;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zrollus.bd.Lib.LocationStorageLib;
import com.zrollus.bd.Lib.ModConfigHelper;
import com.zrollus.bd.Lib.PokedollarHandler;
import com.zrollus.bd.Lib.PermissionCompat;
import com.zrollus.bd.Lib.ItemNameCommand;
import com.zrollus.bd.GUI.ShopkeeperEditorScreenHandler;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ShopkeeperCommands {
    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var root = dispatcher.register(
                literal("shopkeeper")
                        .requires(source -> PermissionCompat.check(source, "shopkeeper.use", 0))
                        .executes(ShopkeeperCommands::help)
                        .then(literal("help").executes(ShopkeeperCommands::help))
                        .then(literal("reload").requires(source -> PermissionCompat.check(source, "shopkeeper.reload", 2)).executes(ShopkeeperCommands::reload))
                        .then(literal("give").requires(source -> PermissionCompat.check(source, "shopkeeper.give", 2))
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(c -> giveCreationItem(c, 1))
                                        .then(argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(c -> giveCreationItem(c, IntegerArgumentType.getInteger(c, "amount"))))))
                        .then(literal("create").executes(ShopkeeperCommands::createUsage)
                                .then(argument("type", StringArgumentType.word())
                                        .suggests((c, b) -> { for (String s : new String[]{"selling", "sell", "buying", "buy", "trading", "trade", "book", "admin"}) b.suggest(s); return b.buildFuture(); })
                                        .executes(ShopkeeperCommands::createDefault)
                                        .then(argument("object", StringArgumentType.greedyString())
                                                .suggests((c, b) -> { b.suggest("player"); b.suggest("sign"); Registries.ENTITY_TYPE.getIds().forEach(id -> { b.suggest(id.toString()); if ("minecraft".equals(id.getNamespace())) b.suggest(id.getPath()); }); return b.buildFuture(); })
                                                .executes(ShopkeeperCommands::create))))
                        .then(literal("edit").executes(ShopkeeperCommands::editNearest).then(shopId().executes(ShopkeeperCommands::edit)))
                        .then(literal("remote").then(anyShopId()
                                .executes(ShopkeeperCommands::remote)
                                .then(argument("player", EntityArgumentType.player()).requires(source -> PermissionCompat.check(source, "shopkeeper.remote.others", 2)).executes(ShopkeeperCommands::remoteOther))))
                        .then(literal("browse").then(anyShopId().executes(ShopkeeperCommands::remote)))
                        .then(literal("list").executes(c -> list(c, 1))
                                .then(argument("page", IntegerArgumentType.integer(1)).executes(c -> list(c, IntegerArgumentType.getInteger(c, "page")))))
                        .then(literal("shops").executes(c -> list(c, 1))
                                .then(argument("page", IntegerArgumentType.integer(1)).executes(c -> list(c, IntegerArgumentType.getInteger(c, "page")))))
                        .then(literal("toggle").then(shopId().executes(ShopkeeperCommands::toggle)))
                        .then(literal("enable").executes(c -> setClosedNearest(c, false)).then(shopId().executes(c -> setClosed(c, false))))
                        .then(literal("disable").executes(c -> setClosedNearest(c, true)).then(shopId().executes(c -> setClosed(c, true))))
                        .then(literal("stock").executes(ShopkeeperCommands::stockNearest).then(shopId().executes(ShopkeeperCommands::stock)))
                        .then(literal("move").executes(ShopkeeperCommands::moveNearest).then(shopId().executes(ShopkeeperCommands::move)))
                        .then(literal("name").then(shopId()
                                .then(argument("name", StringArgumentType.greedyString()).executes(ShopkeeperCommands::name))))
                        .then(literal("rename").then(shopId()
                                .then(argument("name", StringArgumentType.greedyString()).executes(ShopkeeperCommands::name))))
                        .then(literal("skin").then(argument("target", StringArgumentType.word())
                                .suggests(ShopkeeperCommands::suggestSkinTargets)
                                .executes(ShopkeeperCommands::skinNearest)
                                .then(argument("player", StringArgumentType.word())
                                        .suggests((c, b) -> { for (String name : c.getSource().getServer().getPlayerNames()) b.suggest(name); return b.buildFuture(); })
                                        .executes(ShopkeeperCommands::skin))))
                        .then(literal("delete").then(shopId()
                                .executes(ShopkeeperCommands::deleteWarning)
                                .then(literal("confirm").executes(ShopkeeperCommands::delete))))
                        .then(literal("remove").then(shopId()
                                .executes(ShopkeeperCommands::deleteWarning)
                                .then(literal("confirm").executes(ShopkeeperCommands::delete))))
                        .then(literal("removeall").executes(ShopkeeperCommands::removeAllWarning)
                                .then(literal("confirm").executes(ShopkeeperCommands::removeAll)))
                        .then(literal("transfer").then(shopId()
                                .then(argument("player", EntityArgumentType.player()).executes(ShopkeeperCommands::transfer))))
                        .then(literal("settradeperm").requires(source -> PermissionCompat.check(source, "shopkeeper.settradeperm", 2))
                                .then(shopId()
                                        .then(argument("level", IntegerArgumentType.integer(0, 4)).executes(ShopkeeperCommands::setTradePermission))))
                        .then(literal("snapshot").executes(ShopkeeperCommands::snapshotUsage)
                                .then(literal("list").then(shopId().executes(ShopkeeperCommands::snapshotList)))
                                .then(literal("create").then(shopId()
                                        .then(argument("name", StringArgumentType.greedyString()).executes(ShopkeeperCommands::snapshotCreate))))
                                .then(literal("remove").then(shopId()
                                        .then(argument("index", IntegerArgumentType.integer(1)).executes(ShopkeeperCommands::snapshotRemove))))
                                .then(literal("restore").then(shopId()
                                        .then(argument("index", IntegerArgumentType.integer(1)).executes(ShopkeeperCommands::snapshotRestore)))))
                        .then(tradeCommands("trade"))
                        .then(tradeCommands("trades"))
                        .then(memberCommands("member"))
                        .then(memberCommands("members"))
                        .then(literal("notify").then(shopId().executes(ShopkeeperCommands::notifyToggle)))
                        .then(literal("notifications").then(shopId()
                                .executes(ShopkeeperCommands::notificationStatus)
                                .then(literal("on").executes(c -> setNotifications(c, true)))
                                .then(literal("off").executes(c -> setNotifications(c, false)))))
                        .then(literal("exact").then(shopId().executes(ShopkeeperCommands::exactToggle)))
                        .then(literal("matching").then(shopId()
                                .executes(ShopkeeperCommands::matchingStatus)
                                .then(literal("exact").executes(c -> setExactMatching(c, true)))
                                .then(literal("type-only").executes(c -> setExactMatching(c, false)))))
                        .then(literal("sethire").then(shopId()
                                .then(argument("price", LongArgumentType.longArg(-1)).executes(ShopkeeperCommands::setHire))))
                        .then(literal("forhire").then(shopId()
                                .then(argument("price", LongArgumentType.longArg(0)).executes(ShopkeeperCommands::setHire))))
                        .then(literal("notforhire").then(shopId().executes(ShopkeeperCommands::unsetHire)))
                        .then(literal("hire").then(anyShopId().executes(ShopkeeperCommands::hire)))
                        .then(literal("equipment").executes(ShopkeeperCommands::equipmentNearest).then(shopId().executes(ShopkeeperCommands::equipment)))
                        .then(literal("history").executes(c -> history(c, 1))
                                .then(argument("page", IntegerArgumentType.integer(1)).executes(c -> history(c, IntegerArgumentType.getInteger(c, "page")))))
                        .then(literal("teleport").requires(source -> PermissionCompat.check(source, "shopkeeper.teleport", 2))
                                .then(shopId().executes(ShopkeeperCommands::teleport)))
                        .then(literal("tp").requires(source -> PermissionCompat.check(source, "shopkeeper.teleport", 2))
                                .then(shopId().executes(ShopkeeperCommands::teleport)))
                        .then(literal("adopt").requires(source -> PermissionCompat.check(source, "shopkeeper.adopt", 2))
                                .then(argument("entity", EntityArgumentType.entity()).executes(ShopkeeperCommands::adopt)))
                        .then(literal("editvillager").requires(source -> PermissionCompat.check(source, "shopkeeper.editvillager", 2))
                                .then(argument("entity", EntityArgumentType.entity()).executes(ShopkeeperCommands::editVillager)))
            );
            dispatcher.register(literal("shopkeepers").redirect(root));
            dispatcher.register(literal("sk").redirect(root));
        });
    }

    private static RequiredArgumentBuilder<ServerCommandSource, UUID> shopId() {
        return argument("id", UuidArgumentType.uuid()).suggests(ShopkeeperCommands::suggestShopIds);
    }

    private static RequiredArgumentBuilder<ServerCommandSource, UUID> anyShopId() {
        return argument("id", UuidArgumentType.uuid()).suggests(ShopkeeperCommands::suggestAllShopIds);
    }

    private static CompletableFuture<Suggestions> suggestShopIds(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        boolean op = shopAdmin(context.getSource());
        for (Shopkeeper shop : ShopkeeperManager.get(context.getSource().getServer()).all()) {
            if (player != null && !op && !shop.canManage(player.getUuid(), false)) continue;
            builder.suggest(shop.id.toString(), Text.literal(shop.name + " — " + shop.type.name().toLowerCase(Locale.ROOT)));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAllShopIds(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (Shopkeeper shop : ShopkeeperManager.get(context.getSource().getServer()).all())
            builder.suggest(shop.id.toString(), Text.literal(shop.name + " — " + shop.type.name().toLowerCase(Locale.ROOT)));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestSkinTargets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (String name : context.getSource().getServer().getPlayerNames()) builder.suggest(name);
        return suggestShopIds(context, builder);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> tradeCommands(String name) {
        return literal(name).executes(ShopkeeperCommands::tradeUsage)
                .then(literal("add").then(shopId().executes(ShopkeeperCommands::addTrade)))
                .then(literal("remove").then(shopId()
                        .then(argument("index", IntegerArgumentType.integer(1)).executes(ShopkeeperCommands::removeTrade))))
                .then(literal("command").requires(source -> PermissionCompat.check(source, "shopkeeper.trade.command", 2))
                        .then(shopId().then(argument("index", IntegerArgumentType.integer(1))
                                .then(argument("command", StringArgumentType.greedyString()).executes(ShopkeeperCommands::setTradeCommand)))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> memberCommands(String name) {
        return literal(name).executes(ShopkeeperCommands::memberUsage)
                .then(literal("add").then(shopId()
                        .then(argument("player", EntityArgumentType.player()).executes(c -> member(c, true)))))
                .then(literal("level").then(shopId().then(argument("player", EntityArgumentType.player())
                        .then(argument("level", StringArgumentType.word())
                                .suggests((c, b) -> { b.suggest("container"); b.suggest("edit"); b.suggest("full"); return b.buildFuture(); })
                                .executes(ShopkeeperCommands::memberLevel)))))
                .then(literal("remove").then(shopId()
                        .then(argument("player", EntityArgumentType.player()).executes(c -> member(c, false)))));
    }

    private static int help(CommandContext<ServerCommandSource> context) {
        sendDetailedHelp(context.getSource());
        return 1;
    }

    public static void sendDetailedHelp(ServerCommandSource source) {
        source.sendMessage(Text.literal("--- Help: /shopkeeper (aliases: /shopkeepers, /sk) ---")
                .formatted(Formatting.GOLD, Formatting.BOLD));
        helpLine(source, "/sk help", "Shows this complete command guide.");
        helpLine(source, "/sk create <type> [player|sign|mob-id]", "Creates a selling, buying, trading, book, or admin shopkeeper.");
        helpLine(source, "/sk edit [id]", "Opens the editor for the looked-at or selected shopkeeper.");
        helpLine(source, "/sk browse <id>", "Opens a shop remotely as a customer.");
        helpLine(source, "/sk remote <id> [player]", "Remotely opens a shop; operators can open it for another player.");
        helpLine(source, "/sk list|shops [page]", "Lists the shopkeepers you can manage.");
        helpLine(source, "/sk stock [id]", "Opens the stock container for a shopkeeper.");
        helpLine(source, "/sk move [id]", "Moves a shopkeeper to your current position.");
        helpLine(source, "/sk name|rename <id> <name>", "Changes the shopkeeper's plain name; a styled name tag preserves colors and formatting.");
        helpLine(source, "/sk skin [id] <player>", "Changes a player-shaped shopkeeper's skin.");
        helpLine(source, "/sk equipment [id]", "Opens the shopkeeper mob's equipment editor.");
        helpLine(source, "/sk enable|disable [id]", "Opens or closes a shop; toggle <id> switches its current state.");
        helpLine(source, "/sk trade|trades add <id>", "Adds a trade using the items in your hands.");
        helpLine(source, "/sk trades remove <id> <number>", "Removes a numbered trade.");
        helpLine(source, "/sk member|members add <id> <player>", "Adds a member who can access the shop.");
        helpLine(source, "/sk members level <id> <player> <container|edit|full>", "Changes a member's access level.");
        helpLine(source, "/sk members remove <id> <player>", "Removes a shop member.");
        helpLine(source, "/sk notify <id>", "Toggles owner trade notifications.");
        helpLine(source, "/sk notifications <id> [on|off]", "Shows or explicitly changes notification status.");
        helpLine(source, "/sk exact <id>", "Toggles exact item matching for trades.");
        helpLine(source, "/sk matching <id> [exact|type-only]", "Shows or changes how strictly trade items are matched.");
        helpLine(source, "/sk forhire|sethire <id> <price>", "Offers the shopkeeper for hire at the given price.");
        helpLine(source, "/sk notforhire <id>", "Removes a shopkeeper from the hiring market.");
        helpLine(source, "/sk hire <id>", "Hires an available shopkeeper.");
        helpLine(source, "/sk transfer <id> <player>", "Transfers shop ownership to another player.");
        helpLine(source, "/sk snapshot list|create|remove|restore ...", "Manages saved versions of a shop and its trades.");
        helpLine(source, "/sk history [page]", "Shows the shopkeeper activity history.");
        helpLine(source, "/sk remove|delete <id> [confirm]", "Permanently removes one shopkeeper after confirmation.");

        if (shopAdmin(source)) {
            source.sendMessage(Text.literal("Operator commands").formatted(Formatting.DARK_AQUA, Formatting.BOLD));
            helpLine(source, "/sk give <player> [amount]", "Gives shop creation items.");
            helpLine(source, "/sk trades command <id> <number> <command>", "Runs a server command when that trade completes.");
            helpLine(source, "/sk settradeperm <id> <level>", "Sets the permission level required to use a shop.");
            helpLine(source, "/sk teleport|tp <id>", "Teleports to a shopkeeper.");
            helpLine(source, "/sk adopt <entity>", "Turns an existing living entity into an admin shopkeeper.");
            helpLine(source, "/sk editvillager <entity>", "Edits an existing villager's trades.");
            helpLine(source, "/sk removeall [confirm]", "Removes all shopkeepers you own after confirmation.");
            helpLine(source, "/sk reload", "Reloads the shopkeeper configuration.");
        }
    }

    private static void helpLine(ServerCommandSource source, String command, String description) {
        source.sendMessage(Text.literal(command).formatted(Formatting.YELLOW)
                .append(Text.literal(" - ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(description).formatted(Formatting.GRAY)));
    }

    private static int createUsage(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.literal("Usage: /sk create <selling|buying|trading|book|admin> [player|mob-id|sign]").formatted(Formatting.YELLOW));
        context.getSource().sendMessage(Text.literal("For a player shop, look at its stock container first. The mob defaults to a villager.").formatted(Formatting.GRAY));
        return 1;
    }

    private static int tradeUsage(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.literal("Trades are easiest to edit with /sk edit [id].").formatted(Formatting.YELLOW));
        context.getSource().sendMessage(Text.literal("Commands: /sk trades add <id>, remove <id> <number>, command <id> <number> <command>").formatted(Formatting.GRAY));
        return 1;
    }

    private static int memberUsage(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.literal("Usage: /sk members add|level|remove <shop-id> <player> [container|edit|full]").formatted(Formatting.YELLOW));
        return 1;
    }

    private static int snapshotUsage(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.literal("Usage: /sk snapshot list|create|remove|restore <shop-id> [name|number]").formatted(Formatting.YELLOW));
        return 1;
    }

    private static int giveCreationItem(CommandContext<ServerCommandSource> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        ItemStack stack = ShopCreationItem.createStack(amount);
        target.getInventory().offerOrDrop(stack);
        target.sendMessage(Text.literal("Received " + amount + " shop creation item(s). Right-click air to change type; sneak-right-click air to change mob.").formatted(Formatting.GREEN));
        return success(context, "Gave " + amount + " shop creation item(s) to " + target.getName().getString() + ".");
    }

    private static int createDefault(CommandContext<ServerCommandSource> context) {
        return create(context, "minecraft:villager");
    }

    private static int create(CommandContext<ServerCommandSource> context) {
        return create(context, StringArgumentType.getString(context, "object"));
    }

    private static int create(CommandContext<ServerCommandSource> context, String object) {
        ServerPlayerEntity player = player(context);
        if (player == null) return 0;
        String typeName = switch (StringArgumentType.getString(context, "type").toLowerCase(Locale.ROOT)) {
            case "sell" -> "SELLING";
            case "buy" -> "BUYING";
            case "trade" -> "TRADING";
            default -> StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT);
        };
        Shopkeeper.Type type;
        try { type = Shopkeeper.Type.valueOf(typeName); }
        catch (IllegalArgumentException e) { return error(context, "Unknown shop type."); }
        if (type == Shopkeeper.Type.ADMIN && !shopAdmin(context.getSource())) return error(context, "You do not have permission to create admin shops.");

        ShopkeeperManager manager = ShopkeeperManager.get(player.getServer());
        long owned = manager.all().stream().filter(s -> player.getUuid().equals(s.owner)).count();
        int limit = ModConfigHelper.get().maxShopkeepersPerPlayer;
        if (!shopAdmin(player) && owned >= limit) return error(context, "You reached the limit of " + limit + " shopkeepers.");

        ServerWorld world = player.getServerWorld();
        BlockHitResult hit = blockHit(player);
        Shopkeeper shop = new Shopkeeper(UUID.randomUUID());
        shop.type = type;
        shop.owner = type == Shopkeeper.Type.ADMIN ? null : player.getUuid();
        shop.ownerName = type == Shopkeeper.Type.ADMIN ? "Server" : player.getName().getString();
        shop.name = (type == Shopkeeper.Type.ADMIN ? "Admin" : shop.ownerName) + "'s Shop";
        shop.exactItems = ModConfigHelper.get().defaultExactShopItems;
        shop.notifyOwner = ModConfigHelper.get().defaultShopTradeNotifications;
        shop.worldId = world.getRegistryKey().getValue().toString();

        object = normalizeObject(object);
        if (object.equalsIgnoreCase("sign")) {
            if (hit == null || !(world.getBlockEntity(hit.getBlockPos()) instanceof SignBlockEntity)) return error(context, "Look directly at the sign to use as this shopkeeper.");
            shop.signObject = true;
            shop.objectType = "sign";
            shop.objectPos = hit.getBlockPos().toImmutable();
            if (!shop.isAdmin()) {
                BlockPos container = adjacentInventory(world, hit.getBlockPos());
                if (container == null) return error(context, "The sign needs an adjacent inventory for stock.");
                shop.containerPos = container;
                shop.containerWorldId = shop.worldId;
            }
        } else {
            Identifier objectId = Identifier.tryParse(object);
            if (objectId == null) return error(context, "Invalid entity id.");
            if (!shop.isAdmin()) {
                if (hit == null || ShopkeeperSystem.inventoryAt(world, hit.getBlockPos()) == null) return error(context, "Look at the stock container before creating a player shop.");
                shop.containerPos = hit.getBlockPos().toImmutable();
                shop.containerWorldId = shop.worldId;
            }
            Vec3d spawn = player.getPos().add(player.getRotationVec(1.0F).multiply(2.0));
            shop.objectPos = BlockPos.ofFloored(spawn);
            shop.objectType = objectId.toString();
            if (ShopkeeperSystem.isPlayerObject(shop))
                ShopkeeperSystem.usePlayerProfile(shop, player.getGameProfile());
            if (ShopkeeperSystem.spawnObject(world, shop, objectId, spawn) == null) return error(context, "That entity cannot be used as a shopkeeper mob.");
        }

        manager.add(shop);
        player.sendMessage(Text.literal("Created " + shop.type.name().toLowerCase() + " shopkeeper " + shop.id).formatted(Formatting.GREEN));
        ShopkeeperSystem.openEditor(player, shop);
        return 1;
    }

    private static int edit(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context);
        if (shop == null) return 0;
        ShopkeeperSystem.openEditor(player(context), shop);
        return 1;
    }

    private static int editNearest(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = nearestManaged(context); if (shop == null) return 0;
        ShopkeeperSystem.openEditor(player(context), shop);
        return 1;
    }

    private static int remote(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = player(context); if (player == null) return 0;
        Shopkeeper shop = ShopkeeperManager.get(player.getServer()).get(UuidArgumentType.getUuid(context, "id"));
        if (shop == null) return error(context, "Unknown shopkeeper.");
        ShopkeeperSystem.interact(player, shop);
        return 1;
    }

    private static int remoteOther(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Shopkeeper shop = ShopkeeperManager.get(context.getSource().getServer()).get(UuidArgumentType.getUuid(context, "id"));
        if (shop == null) return error(context, "Unknown shopkeeper.");
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        ShopkeeperSystem.interact(target, shop);
        return success(context, "Opened " + shop.name + " for " + target.getName().getString() + ".");
    }

    private static int list(CommandContext<ServerCommandSource> context, int page) {
        ServerPlayerEntity player = player(context);
        if (player == null) return 0;
        int offset = (page - 1) * 10;
        int found = 0, shown = 0;
        for (Shopkeeper shop : ShopkeeperManager.get(player.getServer()).all()) {
            if (!shop.canManage(player.getUuid(), shopAdmin(player))) continue;
            if (found++ < offset) continue;
            if (shown >= 10) continue;
            player.sendMessage(ShopkeeperSystem.button(shop.name + " [" + shop.type.name().toLowerCase() + "]", "/shopkeeper edit " + shop.id, shop.closed ? Formatting.RED : Formatting.GREEN)
                    .append(Text.literal(" " + shop.id).formatted(Formatting.DARK_GRAY)));
            shown++;
        }
        if (found == 0) player.sendMessage(Text.literal("You do not manage any shopkeepers.").formatted(Formatting.YELLOW));
        else player.sendMessage(Text.literal("Page " + page + " — showing " + shown + " shopkeepers.").formatted(Formatting.GRAY));
        return shown;
    }

    private static int toggle(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        shop.closed = !shop.closed; changed(context);
        context.getSource().sendMessage(Text.literal("Shop is now " + (shop.closed ? "closed" : "open") + ".").formatted(Formatting.GREEN)); return 1;
    }

    private static int setClosed(CommandContext<ServerCommandSource> context, boolean closed) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        return setClosed(context, shop, closed);
    }

    private static int setClosedNearest(CommandContext<ServerCommandSource> context, boolean closed) {
        Shopkeeper shop = nearestManaged(context); if (shop == null) return 0;
        return setClosed(context, shop, closed);
    }

    private static int setClosed(CommandContext<ServerCommandSource> context, Shopkeeper shop, boolean closed) {
        shop.closed = closed; changed(context);
        return success(context, "Shop is now " + (closed ? "disabled (closed)." : "enabled (open)."));
    }

    private static int stock(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        return openStock(context, shop);
    }

    private static int stockNearest(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = nearestManaged(context); if (shop == null) return 0;
        return openStock(context, shop);
    }

    private static int openStock(CommandContext<ServerCommandSource> context, Shopkeeper shop) {
        if (shop.containerPos == null) return error(context, "Admin shops do not have stock containers.");
        ServerWorld world = ShopkeeperSystem.world(context.getSource().getServer(), shop.containerWorldId);
        BlockEntity blockEntity = world == null ? null : world.getBlockEntity(shop.containerPos);
        if (blockEntity instanceof NamedScreenHandlerFactory factory) { player(context).openHandledScreen(factory); return 1; }
        return error(context, "The stock container is missing.");
    }

    private static int move(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        return move(context, shop);
    }

    private static int moveNearest(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = nearestManaged(context); if (shop == null) return 0;
        return move(context, shop);
    }

    private static int move(CommandContext<ServerCommandSource> context, Shopkeeper shop) {
        if (shop.signObject) return error(context, "Sign shopkeepers are moved by recreating them on another sign.");
        ServerPlayerEntity player = player(context);
        if (!ShopkeeperSystem.beginMove(player, shop)) return error(context, "Shopkeeper entity is not loaded.");
        return 1;
    }

    private static int name(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        shop.setName(ItemNameCommand.parseLegacyFormatting(StringArgumentType.getString(context, "name")));
        Entity entity = findEntity(context.getSource().getServer(), shop.entityId); if (entity != null) entity.setCustomName(shop.getDisplayName());
        boolean changedSkin = ShopkeeperSystem.isPlayerObject(shop)
                && ShopkeeperSystem.setPlayerSkin(context.getSource().getServer(), shop, shop.name);
        changed(context); return success(context, changedSkin ? "Shopkeeper renamed; its player skin is updating." : "Shopkeeper renamed.");
    }

    private static int skinNearest(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = nearestManaged(context); if (shop == null) return 0;
        return setSkin(context, shop, StringArgumentType.getString(context, "target"));
    }

    private static int skin(CommandContext<ServerCommandSource> context) {
        UUID id;
        try { id = UUID.fromString(StringArgumentType.getString(context, "target")); }
        catch (IllegalArgumentException ignored) { return error(context, "Use /sk skin <shop-id> <player-name>, or stand near a shop and use /sk skin <player-name>."); }
        ServerPlayerEntity player = player(context); if (player == null) return 0;
        Shopkeeper shop = ShopkeeperManager.get(player.getServer()).get(id);
        if (shop == null) return error(context, "Unknown shopkeeper.");
        if (!shop.canManage(player.getUuid(), shopAdmin(player))) return error(context, "You cannot manage that shopkeeper.");
        return setSkin(context, shop, StringArgumentType.getString(context, "player"));
    }

    private static int setSkin(CommandContext<ServerCommandSource> context, Shopkeeper shop, String playerName) {
        if (!ShopkeeperSystem.isPlayerObject(shop)) return error(context, "Only player-shaped shopkeepers have player skins.");
        if (!ShopkeeperSystem.setPlayerSkin(context.getSource().getServer(), shop, playerName))
            return error(context, "Enter a valid Minecraft player name (3-16 letters, numbers, or underscores).");
        return success(context, "Using " + playerName + " for this shopkeeper; its skin will update when the profile lookup finishes.");
    }

    private static String normalizeObject(String object) {
        String value = object == null ? "" : object.trim().toLowerCase(Locale.ROOT);
        if (value.equals("player") || value.equals("bd:player") || value.equals("minecraft:player")) return "bd:player_shopkeeper";
        if (value.equals("sign")) return value;
        return value.contains(":") ? value : "minecraft:" + value;
    }

    private static int deleteWarning(CommandContext<ServerCommandSource> context) {
        UUID id = UuidArgumentType.getUuid(context, "id");
        if (managed(context) == null) return 0;
        context.getSource().sendMessage(Text.literal("This permanently removes the shopkeeper. Click ").formatted(Formatting.RED)
                .append(ShopkeeperSystem.button("[confirm delete]", "/shopkeeper delete " + id + " confirm", Formatting.DARK_RED)));
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        if (!shop.canFullyManage(player(context).getUuid(), shopAdmin(context.getSource())))
            return error(context, "Only the owner or a full-access member can delete this shop.");
        ShopkeeperSystem.removeObject(context.getSource().getServer(), shop);
        ShopkeeperManager.get(context.getSource().getServer()).remove(shop.id);
        return success(context, "Shopkeeper deleted. Its stock container and contents were not removed.");
    }

    private static int removeAllWarning(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = player(context); if (player == null) return 0;
        long count = ShopkeeperManager.get(player.getServer()).all().stream().filter(s -> player.getUuid().equals(s.owner)).count();
        context.getSource().sendMessage(Text.literal("This removes all " + count + " shopkeepers you own. Click ").formatted(Formatting.RED)
                .append(ShopkeeperSystem.button("[confirm remove all]", "/shopkeeper removeall confirm", Formatting.DARK_RED)));
        return 1;
    }

    private static int removeAll(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = player(context); if (player == null) return 0;
        ShopkeeperManager manager = ShopkeeperManager.get(player.getServer());
        var owned = manager.all().stream().filter(s -> player.getUuid().equals(s.owner)).toList();
        for (Shopkeeper shop : owned) {
            ShopkeeperSystem.removeObject(player.getServer(), shop);
            manager.remove(shop.id);
        }
        return success(context, "Removed " + owned.size() + " owned shopkeepers. Stock containers were preserved.");
    }

    private static int transfer(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        ServerPlayerEntity sender = player(context);
        if (shop.isAdmin()) return error(context, "Admin shops do not have player ownership.");
        if (!shop.canFullyManage(sender.getUuid(), shopAdmin(context.getSource())))
            return error(context, "Only the owner or a full-access member can transfer this shop.");
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        shop.members.remove(target.getUuid());
        shop.owner = target.getUuid();
        shop.ownerName = target.getName().getString();
        changed(context);
        target.sendMessage(Text.literal(sender.getName().getString() + " transferred " + shop.name + " to you.").formatted(Formatting.GREEN));
        return success(context, "Transferred " + shop.name + " to " + target.getName().getString() + ".");
    }

    private static int setTradePermission(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = ShopkeeperManager.get(context.getSource().getServer()).get(UuidArgumentType.getUuid(context, "id"));
        if (shop == null) return error(context, "Unknown shopkeeper.");
        if (!shop.isAdmin()) return error(context, "Trade permission levels are only available for admin shops.");
        shop.tradePermissionLevel = IntegerArgumentType.getInteger(context, "level");
        changed(context);
        return success(context, shop.tradePermissionLevel == 0 ? "Trade permission restriction removed."
                : "Trading now requires vanilla permission level " + shop.tradePermissionLevel + ".");
    }

    private static Shopkeeper fullManaged(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return null;
        ServerPlayerEntity player = player(context);
        if (!shop.canFullyManage(player.getUuid(), shopAdmin(context.getSource()))) {
            error(context, "This action requires owner or full member access.");
            return null;
        }
        return shop;
    }

    private static int snapshotList(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = fullManaged(context); if (shop == null) return 0;
        if (shop.snapshots.isEmpty()) return success(context, "This shop has no snapshots.");
        for (int i = 0; i < shop.snapshots.size(); i++) {
            ShopkeeperSnapshot snapshot = shop.snapshots.get(i);
            context.getSource().sendMessage(Text.literal("#" + (i + 1) + " " + snapshot.name + " — "
                    + Instant.ofEpochMilli(snapshot.createdAt)).formatted(Formatting.GRAY));
        }
        return shop.snapshots.size();
    }

    private static int snapshotCreate(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = fullManaged(context); if (shop == null) return 0;
        if (shop.snapshots.size() >= 10) return error(context, "This shop already has the maximum of 10 snapshots.");
        String name = StringArgumentType.getString(context, "name").trim();
        if (name.isEmpty()) return error(context, "Snapshot name cannot be empty.");
        shop.snapshots.add(ShopkeeperSnapshot.capture(shop, name));
        changed(context);
        return success(context, "Created snapshot #" + shop.snapshots.size() + " (“" + name + "”).");
    }

    private static int snapshotRemove(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = fullManaged(context); if (shop == null) return 0;
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        if (index < 0 || index >= shop.snapshots.size()) return error(context, "Snapshot index does not exist.");
        ShopkeeperSnapshot removed = shop.snapshots.remove(index);
        changed(context);
        return success(context, "Removed snapshot “" + removed.name + "”.");
    }

    private static int snapshotRestore(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = fullManaged(context); if (shop == null) return 0;
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        if (index < 0 || index >= shop.snapshots.size()) return error(context, "Snapshot index does not exist.");
        ShopkeeperSnapshot snapshot = shop.snapshots.get(index);
        snapshot.apply(shop);
        Entity entity = findEntity(context.getSource().getServer(), shop.entityId);
        if (entity != null) entity.setCustomName(shop.getDisplayName());
        changed(context);
        return success(context, "Restored snapshot “" + snapshot.name + "”.");
    }

    private static int addTrade(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        ServerPlayerEntity player = player(context);
        ItemStack first = player.getInventory().getStack(0);
        ItemStack second = player.getInventory().getStack(1);
        ItemStack result = player.getInventory().getStack(2);
        if (first.isEmpty() || result.isEmpty()) return error(context, "Put the first cost in hotbar slot 1 and result in slot 3. Slot 2 is optional.");
        first = ShopkeeperPlaceholderItems.replace(first, shop.isAdmin());
        second = ShopkeeperPlaceholderItems.replace(second, shop.isAdmin());
        result = ShopkeeperPlaceholderItems.replace(result, shop.isAdmin());
        shop.trades.add(new ShopkeeperTrade(first, second, result)); changed(context);
        return success(context, "Trade added: " + ShopkeeperMerchant.describe(first) + " → " + ShopkeeperMerchant.describe(result));
    }

    private static int removeTrade(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        if (index < 0 || index >= shop.trades.size()) return error(context, "Trade index does not exist.");
        shop.trades.remove(index); changed(context); return success(context, "Trade removed.");
    }

    private static int setTradeCommand(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        if (index < 0 || index >= shop.trades.size()) return error(context, "Trade index does not exist.");
        shop.trades.get(index).command = StringArgumentType.getString(context, "command"); changed(context);
        return success(context, "Command attached. {player} and {uuid} are supported.");
    }

    private static int member(CommandContext<ServerCommandSource> context, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        if (!shop.canFullyManage(player(context).getUuid(), shopAdmin(context.getSource())))
            return error(context, "Only the owner or a full-access member can manage members.");
        if (add && !shop.members.containsKey(target.getUuid()) && shop.members.size() >= ModConfigHelper.get().maxShopMembers)
            return error(context, "This shop already has the maximum of " + ModConfigHelper.get().maxShopMembers + " members.");
        if (add) shop.members.put(target.getUuid(), Shopkeeper.AccessLevel.EDIT); else shop.members.remove(target.getUuid());
        changed(context); return success(context, target.getName().getString() + (add ? " now has edit access." : " was removed from this shop."));
    }

    private static int memberLevel(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        if (!shop.canFullyManage(player(context).getUuid(), shopAdmin(context.getSource())))
            return error(context, "Only the owner or a full-access member can manage members.");
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        if (!shop.members.containsKey(target.getUuid())) return error(context, "That player is not a shop member.");
        Shopkeeper.AccessLevel level;
        try { level = Shopkeeper.AccessLevel.valueOf(StringArgumentType.getString(context, "level").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return error(context, "Access must be container, edit, or full."); }
        if (level == Shopkeeper.AccessLevel.NONE) return error(context, "Use member remove to revoke access.");
        shop.members.put(target.getUuid(), level);
        changed(context);
        if (target.currentScreenHandler instanceof ShopkeeperEditorScreenHandler) target.closeHandledScreen();
        return success(context, target.getName().getString() + " now has " + level.name().toLowerCase(Locale.ROOT) + " access.");
    }

    private static int notifyToggle(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0; shop.notifyOwner = !shop.notifyOwner; changed(context);
        return success(context, "Trade notifications " + (shop.notifyOwner ? "enabled." : "disabled."));
    }

    private static int notificationStatus(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        return success(context, "Trade notifications are " + (shop.notifyOwner ? "on." : "off."));
    }

    private static int setNotifications(CommandContext<ServerCommandSource> context, boolean enabled) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        shop.notifyOwner = enabled; changed(context);
        return success(context, "Trade notifications turned " + (enabled ? "on." : "off."));
    }

    private static int exactToggle(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0; shop.exactItems = !shop.exactItems; changed(context);
        return success(context, "Exact item-component matching " + (shop.exactItems ? "enabled." : "disabled."));
    }

    private static int matchingStatus(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        return success(context, "Item matching is " + (shop.exactItems ? "exact (components included)." : "type-only."));
    }

    private static int setExactMatching(CommandContext<ServerCommandSource> context, boolean exact) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        shop.exactItems = exact; changed(context);
        return success(context, "Item matching set to " + (exact ? "exact." : "type-only."));
    }

    private static int setHire(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        if (shop.isAdmin()) return error(context, "Admin shops cannot be hired.");
        shop.hirePrice = LongArgumentType.getLong(context, "price");
        if (shop.hirePrice >= 0) {
            if (shop.owner != null) shop.hireSeller = shop.owner;
            shop.owner = null;
            shop.ownerName = "For Hire";
        } else if (shop.owner == null && shop.hireSeller != null) {
            shop.owner = shop.hireSeller;
            shop.hireSeller = null;
            ServerPlayerEntity restored = context.getSource().getServer().getPlayerManager().getPlayer(shop.owner);
            shop.ownerName = restored == null ? "Owner" : restored.getName().getString();
        }
        changed(context); return success(context, shop.hirePrice < 0 ? "Hiring disabled." : "Shop offered for hire at ₱" + shop.hirePrice + ".");
    }

    private static int unsetHire(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        if (shop.isAdmin()) return error(context, "Admin shops cannot be hired.");
        shop.hirePrice = -1;
        if (shop.owner == null && shop.hireSeller != null) {
            shop.owner = shop.hireSeller;
            shop.hireSeller = null;
            ServerPlayerEntity restored = context.getSource().getServer().getPlayerManager().getPlayer(shop.owner);
            shop.ownerName = restored == null ? "Owner" : restored.getName().getString();
        }
        changed(context);
        return success(context, "This shop is no longer for hire.");
    }

    private static int hire(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity buyer = player(context); if (buyer == null) return 0;
        Shopkeeper shop = ShopkeeperManager.get(buyer.getServer()).get(UuidArgumentType.getUuid(context, "id"));
        if (shop == null || shop.owner != null || shop.hirePrice < 0 || shop.isAdmin()) return error(context, "That shopkeeper is not for hire.");
        PokedollarHandler.syncPhysicalToVirtual(buyer);
        PlayerDataModel data = LocationStorageLib.getPlayerData(buyer.getServer(), buyer.getUuid());
        if (data.bal < shop.hirePrice) return error(context, "You need ₱" + (shop.hirePrice - data.bal) + " more.");
        data.bal -= shop.hirePrice; LocationStorageLib.savePlayerData(buyer.getServer(), buyer.getUuid(), data);
        if (shop.hireSeller != null) {
            PlayerDataModel sellerData = LocationStorageLib.getPlayerData(buyer.getServer(), shop.hireSeller);
            sellerData.bal += shop.hirePrice;
            LocationStorageLib.savePlayerData(buyer.getServer(), shop.hireSeller, sellerData);
        }
        shop.owner = buyer.getUuid(); shop.ownerName = buyer.getName().getString(); shop.hirePrice = -1; shop.hireSeller = null; changed(context);
        return success(context, "You hired " + shop.name + ".");
    }

    private static int equipment(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = managed(context); if (shop == null) return 0;
        return equipment(context, shop);
    }

    private static int equipmentNearest(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = nearestManaged(context); if (shop == null) return 0;
        return equipment(context, shop);
    }

    private static int equipment(CommandContext<ServerCommandSource> context, Shopkeeper shop) {
        Entity entity = findEntity(context.getSource().getServer(), shop.entityId);
        if (!(entity instanceof LivingEntity living)) return error(context, "This shopkeeper has no equipment slots.");
        ServerPlayerEntity player = player(context);
        for (EquipmentSlot slot : EquipmentSlot.values()) living.equipStack(slot, player.getEquippedStack(slot).copy());
        ShopkeeperSystem.captureObjectData(shop, living); changed(context);
        return success(context, "Copied your visible equipment to the shopkeeper.");
    }

    private static int history(CommandContext<ServerCommandSource> context, int page) {
        var history = ShopkeeperManager.get(context.getSource().getServer()).history();
        if (history.isEmpty()) return success(context, "No shopkeeper trades have been recorded.");
        int start = (page - 1) * 10;
        if (start >= history.size()) return error(context, "That history page is empty.");
        for (int i = start; i < Math.min(start + 10, history.size()); i++) context.getSource().sendMessage(Text.literal(history.get(i)).formatted(Formatting.GRAY));
        context.getSource().sendMessage(Text.literal("History page " + page + " / " + ((history.size() + 9) / 10)).formatted(Formatting.GOLD));
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> context) {
        ModConfigHelper.load();
        return success(context, "Builder's Delight shopkeeper configuration reloaded.");
    }

    private static int teleport(CommandContext<ServerCommandSource> context) {
        Shopkeeper shop = ShopkeeperManager.get(context.getSource().getServer()).get(UuidArgumentType.getUuid(context, "id"));
        if (shop == null) return error(context, "Unknown shopkeeper.");
        ServerWorld world = ShopkeeperSystem.world(context.getSource().getServer(), shop.worldId); if (world == null) return error(context, "Shop world is unavailable.");
        player(context).teleport(world, shop.objectPos.getX() + .5, shop.objectPos.getY() + 1, shop.objectPos.getZ() + .5, 0, 0);
        return 1;
    }

    private static int adopt(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "entity");
        if (!(entity instanceof LivingEntity)) return error(context, "Only living mobs can be adopted.");
        ShopkeeperManager manager = ShopkeeperManager.get(context.getSource().getServer());
        if (manager.byEntity(entity.getUuid()) != null) return error(context, "That entity is already a shopkeeper.");
        Shopkeeper shop = new Shopkeeper(UUID.randomUUID()); shop.type = Shopkeeper.Type.ADMIN; shop.ownerName = "Server";
        shop.setName(entity.getName()); shop.entityId = entity.getUuid(); shop.objectType = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        shop.worldId = entity.getWorld().getRegistryKey().getValue().toString(); shop.objectPos = entity.getBlockPos();
        entity.addCommandTag(ShopkeeperSystem.ENTITY_TAG); entity.addCommandTag("bd_shopkeeper_" + shop.id); entity.setInvulnerable(true); entity.setCustomNameVisible(true);
        if (entity instanceof net.minecraft.entity.mob.MobEntity mob) { mob.setAiDisabled(true); mob.setPersistent(); }
        ShopkeeperSystem.captureObjectData(shop, entity);
        if (entity instanceof Merchant merchant) {
            for (TradeOffer offer : merchant.getOffers()) shop.trades.add(new ShopkeeperTrade(offer.getOriginalFirstBuyItem(), offer.getDisplayedSecondBuyItem(), offer.getSellItem()));
        }
        manager.add(shop); return success(context, "Adopted entity as an admin shopkeeper with " + shop.trades.size() + " imported trades.");
    }

    private static int editVillager(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "entity");
        if (!(entity instanceof MerchantEntity merchant)) return error(context, "Target a regular villager or wandering trader.");
        if (ShopkeeperManager.get(context.getSource().getServer()).byEntity(entity.getUuid()) != null)
            return error(context, "That entity is already a shopkeeper; use its normal editor.");
        ShopkeeperSystem.openVillagerEditor(player(context), merchant);
        return 1;
    }

    private static Shopkeeper managed(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = player(context); if (player == null) return null;
        Shopkeeper shop = ShopkeeperManager.get(player.getServer()).get(UuidArgumentType.getUuid(context, "id"));
        if (shop == null) { error(context, "Unknown shopkeeper."); return null; }
        if (!shop.canManage(player.getUuid(), shopAdmin(player))) { error(context, "You cannot manage that shopkeeper."); return null; }
        return shop;
    }

    private static Shopkeeper nearestManaged(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = player(context); if (player == null) return null;
        Shopkeeper shop = ShopkeeperSystem.nearestManaged(player, 8.0);
        if (shop == null) error(context, "No shopkeeper you can edit is within 8 blocks. Use /sk shops to pick one by name.");
        return shop;
    }

    private static ServerPlayerEntity player(CommandContext<ServerCommandSource> context) {
        try { return context.getSource().getPlayerOrThrow(); }
        catch (Exception e) { context.getSource().sendError(Text.literal("This command must be used by a player.")); return null; }
    }

    private static BlockHitResult blockHit(ServerPlayerEntity player) {
        HitResult hit = player.raycast(6.0, 0.0F, false); return hit instanceof BlockHitResult block ? block : null;
    }

    private static BlockPos adjacentInventory(ServerWorld world, BlockPos sign) {
        for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) if (ShopkeeperSystem.inventoryAt(world, sign.offset(direction)) != null) return sign.offset(direction);
        return null;
    }

    private static Entity findEntity(MinecraftServer server, UUID id) {
        if (id == null) return null; for (ServerWorld world : server.getWorlds()) { Entity entity = world.getEntity(id); if (entity != null) return entity; } return null;
    }

    private static void changed(CommandContext<ServerCommandSource> context) { ShopkeeperManager.get(context.getSource().getServer()).changed(); }
    private static int success(CommandContext<ServerCommandSource> context, String message) { context.getSource().sendMessage(Text.literal(message).formatted(Formatting.GREEN)); return 1; }
    private static int error(CommandContext<ServerCommandSource> context, String message) { context.getSource().sendError(Text.literal(message)); return 0; }

    private static boolean shopAdmin(ServerCommandSource source) {
        return PermissionCompat.check(source, "shopkeeper.admin", 2);
    }

    private static boolean shopAdmin(ServerPlayerEntity player) {
        return PermissionCompat.check(player, "shopkeeper.admin", player.hasPermissionLevel(2));
    }

    private ShopkeeperCommands() {}
}
