package com.zrollus.bd.shopkeeper;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.zrollus.bd.Entity.PlayerShopkeeperEntity;
import com.zrollus.bd.Lib.ModConfigHelper;
import com.zrollus.bd.Lib.PermissionCompat;
import com.zrollus.bd.GUI.ShopkeeperEditorScreenHandler;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopkeeperSystem {
    public static final String ENTITY_TAG = "bd_shopkeeper";
    private static final Map<UUID, UUID> PENDING_MOVES = new ConcurrentHashMap<>();
    private static int recoveryTicks;

    public static void register() {
        ShopkeeperCommands.register();
        ServerTickEvents.END_SERVER_TICK.register(ShopkeeperSystem::recoverObjects);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PENDING_MOVES.remove(handler.player.getUuid()));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND || !entity.getCommandTags().contains(ENTITY_TAG)) return ActionResult.PASS;
            if (world.isClient) return ActionResult.SUCCESS;
            Shopkeeper shop = ShopkeeperManager.get(player.getServer()).byEntity(entity.getUuid());
            if (shop == null) return ActionResult.PASS;
            if (tryRenameWithNameTag((ServerPlayerEntity) player, shop, entity, hand)) return ActionResult.SUCCESS;
            interact((ServerPlayerEntity) player, shop);
            return ActionResult.SUCCESS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                entity.getCommandTags().contains(ENTITY_TAG) ? ActionResult.FAIL : ActionResult.PASS);

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient) return ActionResult.PASS;
            String worldId = world.getRegistryKey().getValue().toString();
            ShopkeeperManager manager = ShopkeeperManager.get(player.getServer());
            Shopkeeper shop = manager.bySign(worldId, pos);
            if (shop == null) shop = manager.byContainer(worldId, pos);
            if (shop == null) return ActionResult.PASS;
            if (!shop.signObject && !ModConfigHelper.get().protectShopContainers) return ActionResult.PASS;
            player.sendMessage(Text.literal("Protected shop block. Delete or move the shopkeeper first.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND || world.isClient) return ActionResult.PASS;
            MinecraftServer server = player.getServer();
            UUID movingShopId = PENDING_MOVES.remove(player.getUuid());
            if (movingShopId != null) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                if (player.isSneaking()) {
                    player.sendMessage(Text.literal("Shopkeeper move cancelled.").formatted(Formatting.YELLOW), true);
                    return ActionResult.SUCCESS;
                }
                Shopkeeper movingShop = ShopkeeperManager.get(server).get(movingShopId);
                if (movingShop == null || !movingShop.canManage(player.getUuid(), shopAdmin(serverPlayer))) {
                    player.sendMessage(Text.literal("That shopkeeper is no longer available.").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                BlockPos destination = hit.getBlockPos().up();
                if (!hasPlacementRoom((ServerWorld) world, destination)) {
                    player.sendMessage(Text.literal("The two blocks above the selected block must be clear.").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                if (!moveObject(serverPlayer, movingShop, (ServerWorld) world, destination)) {
                    player.sendMessage(Text.literal("The shopkeeper entity is not loaded. Move closer and try again.").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                player.sendMessage(Text.literal("Shopkeeper moved to " + destination.toShortString() + ".").formatted(Formatting.GREEN), true);
                return ActionResult.SUCCESS;
            }
            String worldId = world.getRegistryKey().getValue().toString();
            ShopkeeperManager manager = ShopkeeperManager.get(server);
            Shopkeeper sign = manager.bySign(worldId, hit.getBlockPos());
            if (sign != null) {
                if (tryRenameWithNameTag((ServerPlayerEntity) player, sign, null, hand)) return ActionResult.SUCCESS;
                interact((ServerPlayerEntity) player, sign);
                return ActionResult.SUCCESS;
            }
            Shopkeeper stockShop = manager.byContainer(worldId, hit.getBlockPos());
            if (ModConfigHelper.get().protectShopContainers && stockShop != null
                    && !stockShop.canAccessContainer(player.getUuid(), shopAdmin((ServerPlayerEntity) player))) {
                player.sendMessage(Text.literal("That container is protected shop stock.").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    public static boolean beginMove(ServerPlayerEntity player, Shopkeeper shop) {
        if (shop == null || shop.signObject || shop.entityId == null) return false;
        Entity entity = findEntity(player.getServer(), shop.entityId);
        if (entity == null) return false;
        PENDING_MOVES.put(player.getUuid(), shop.id);
        player.closeHandledScreen();
        player.sendMessage(Text.literal("Right-click the block the shopkeeper should stand above. Sneak-right-click to cancel.")
                .formatted(Formatting.YELLOW), false);
        return true;
    }

    public static boolean hasPlacementRoom(ServerWorld world, BlockPos feetPos) {
        return world.getBlockState(feetPos).getCollisionShape(world, feetPos).isEmpty()
                && world.getBlockState(feetPos.up()).getCollisionShape(world, feetPos.up()).isEmpty();
    }

    private static boolean moveObject(ServerPlayerEntity player, Shopkeeper shop, ServerWorld destinationWorld, BlockPos destination) {
        Entity entity = findEntity(player.getServer(), shop.entityId);
        if (entity == null) return false;
        double x = destination.getX() + 0.5;
        double y = destination.getY();
        double z = destination.getZ() + 0.5;
        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        float facingPlayer = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        entity.teleport(destinationWorld, x, y, z, Set.of(), facingPlayer, entity.getPitch());
        shop.worldId = destinationWorld.getRegistryKey().getValue().toString();
        shop.objectPos = destination.toImmutable();
        ShopkeeperManager.get(player.getServer()).changed();
        return true;
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        if (entityId == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    public static void interact(ServerPlayerEntity player, Shopkeeper shop) {
        boolean manager = shop.canManage(player.getUuid(), shopAdmin(player));
        if (player.isSneaking() && manager) {
            openEditor(player, shop);
            return;
        }
        if (shop.hirePrice >= 0 && shop.owner == null && !shop.isAdmin()) {
            player.sendMessage(button("Hire " + shop.name + " for ₱" + shop.hirePrice,
                    "/shopkeeper hire " + shop.id, Formatting.GOLD));
            return;
        }
        if (shop.closed) {
            player.sendMessage(Text.literal("This shop is currently closed.").formatted(Formatting.RED), true);
            return;
        }
        if (shop.tradePermissionLevel > 0 && !player.hasPermissionLevel(shop.tradePermissionLevel)) {
            player.sendMessage(Text.literal("You do not have permission to trade with this shopkeeper.").formatted(Formatting.RED), true);
            return;
        }
        if (ModConfigHelper.get().preventTradingWithOwnShop && shop.isOwnerOrMember(player.getUuid())) {
            player.sendMessage(Text.literal("You cannot trade with your own shop. Sneak-use it to edit.").formatted(Formatting.YELLOW), true);
            return;
        }
        if (shop.trades.isEmpty()) {
            player.sendMessage(Text.literal("This shopkeeper has no trades yet.").formatted(Formatting.YELLOW), true);
            return;
        }
        ShopkeeperMerchant merchant = new ShopkeeperMerchant(player.getServer(), shop);
        if (merchant.getOffers().isEmpty()) {
            boolean hasCompleteTrade = shop.trades.stream()
                    .anyMatch(trade -> !trade.costOne.isEmpty() && !trade.result.isEmpty());
            player.sendMessage(Text.literal(hasCompleteTrade
                    ? "This shopkeeper is out of stock."
                    : "This shopkeeper has no complete trades. Set Sold and Cost 1 in the editor.")
                    .formatted(hasCompleteTrade ? Formatting.RED : Formatting.YELLOW), true);
            return;
        }
        // MerchantScreenHandler remains usable only while this merchant identifies
        // the player as its active customer. Vanilla merchant entities do this
        // before opening their offers; this standalone merchant must do it too.
        merchant.setCustomer(player);
        merchant.sendOffers(player, shop.getDisplayName(), 1);
    }

    public static void openEditor(ServerPlayerEntity player, Shopkeeper shop) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new ShopkeeperEditorScreenHandler(syncId, inventory, shop),
                shop.getDisplayName().append(Text.literal(" — Trades"))));
    }

    private static boolean tryRenameWithNameTag(ServerPlayerEntity player, Shopkeeper shop, Entity entity, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (!stack.isOf(Items.NAME_TAG) || customName == null) return false;
        if (!shop.canManage(player.getUuid(), shopAdmin(player))) {
            player.sendMessage(Text.literal("You cannot rename this shopkeeper.").formatted(Formatting.RED), true);
            return true;
        }

        shop.setName(customName);
        if (entity != null) {
            entity.setCustomName(shop.getDisplayName());
            entity.setCustomNameVisible(true);
        }
        if (isPlayerObject(shop)) setPlayerSkin(player.getServer(), shop, shop.name);
        ShopkeeperManager.get(player.getServer()).changed();
        if (!player.isCreative()) stack.decrement(1);
        player.sendMessage(Text.literal("Shopkeeper renamed to " + shop.name + ".").formatted(Formatting.GREEN), true);
        return true;
    }

    public static void openVillagerEditor(ServerPlayerEntity player, MerchantEntity entity) {
        Shopkeeper editing = new Shopkeeper(UUID.randomUUID());
        editing.type = Shopkeeper.Type.ADMIN;
        editing.ownerName = "Server";
        editing.setName(entity.getName());
        editing.objectType = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        editing.entityId = entity.getUuid();
        editing.worldId = entity.getWorld().getRegistryKey().getValue().toString();
        editing.objectPos = entity.getBlockPos();
        for (TradeOffer offer : entity.getOffers())
            editing.trades.add(new ShopkeeperTrade(offer.getOriginalFirstBuyItem(), offer.getDisplayedSecondBuyItem(), offer.getSellItem()));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new ShopkeeperEditorScreenHandler(syncId, inventory, editing, () -> {
                    Entity current = entity.getWorld().getEntityById(entity.getId());
                    if (!(current instanceof Merchant merchant)) return;
                    merchant.getOffers().clear();
                    for (ShopkeeperTrade trade : editing.trades) {
                        if (trade.costOne.isEmpty() || trade.result.isEmpty()) continue;
                        TradedItem first = exactTradedItem(trade.costOne);
                        Optional<TradedItem> second = trade.costTwo.isEmpty() ? Optional.empty() : Optional.of(exactTradedItem(trade.costTwo));
                        merchant.getOffers().add(new TradeOffer(first, second, trade.result.copy(), 0, 999_999, 0, 0.0F));
                    }
                    current.setCustomName(editing.getDisplayName());
                }), editing.getDisplayName().append(Text.literal(" — Villager Editor"))));
    }

    private static TradedItem exactTradedItem(net.minecraft.item.ItemStack stack) {
        return new TradedItem(stack.getItem().getRegistryEntry(), stack.getCount(),
                ComponentPredicate.of(stack.getComponents()), stack.copy());
    }

    public static void showEditor(ServerPlayerEntity player, Shopkeeper shop) {
        player.sendMessage(Text.literal("— " + shop.name + " —").formatted(Formatting.GOLD, Formatting.BOLD));
        player.sendMessage(Text.literal("ID: " + shop.id + " | " + shop.type.name().toLowerCase()
                + " | " + (shop.closed ? "closed" : "open")).formatted(Formatting.GRAY));
        MutableText actions = button(shop.closed ? "[Enable]" : "[Disable]", "/sk " + (shop.closed ? "enable " : "disable ") + shop.id, Formatting.GREEN)
                .append(Text.literal(" "))
                .append(button("[Stock]", "/shopkeeper stock " + shop.id, Formatting.AQUA))
                .append(Text.literal(" "))
                .append(button("[Move]", "/shopkeeper move " + shop.id, Formatting.YELLOW))
                .append(Text.literal(" "))
                .append(button("[Remove]", "/sk remove " + shop.id, Formatting.RED));
        player.sendMessage(actions);
        player.sendMessage(Text.literal("Trades use hotbar slots 1=first cost, 2=optional second cost, 3=result. Then run ")
                .formatted(Formatting.GRAY)
                .append(button("[Add trade]", "/sk trades add " + shop.id, Formatting.GREEN)));
        for (int i = 0; i < shop.trades.size(); i++) {
            ShopkeeperTrade trade = shop.trades.get(i);
            player.sendMessage(Text.literal("#" + (i + 1) + " " + ShopkeeperMerchant.describe(trade.costOne)
                            + (trade.costTwo.isEmpty() ? "" : " + " + ShopkeeperMerchant.describe(trade.costTwo))
                            + " → " + ShopkeeperMerchant.describe(trade.result) + " ").formatted(Formatting.WHITE)
                    .append(button("[remove]", "/sk trades remove " + shop.id + " " + (i + 1), Formatting.RED)));
        }
    }

    static MutableText button(String label, String command, Formatting color) {
        return Text.literal(label).formatted(color).styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
    }

    public static Shopkeeper nearestManaged(ServerPlayerEntity player, double range) {
        double max = range * range;
        return ShopkeeperManager.get(player.getServer()).all().stream()
                .filter(shop -> shop.canManage(player.getUuid(), shopAdmin(player)))
                .filter(shop -> shop.worldId.equals(player.getWorld().getRegistryKey().getValue().toString()))
                .filter(shop -> squaredDistance(player.getPos(), shop.objectPos) <= max)
                .min(Comparator.comparingDouble(shop -> squaredDistance(player.getPos(), shop.objectPos)))
                .orElse(null);
    }

    public static Entity spawnObject(ServerWorld world, Shopkeeper shop, Identifier entityId, Vec3d pos) {
        if (!Registries.ENTITY_TYPE.containsId(entityId)) return null;
        EntityType<?> type = Registries.ENTITY_TYPE.get(entityId);
        Entity entity = type.create(world);
        if (!(entity instanceof LivingEntity living)) return null;
        if (!shop.objectData.isEmpty()) living.readNbt(shop.objectData.copy());
        living.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        if (living instanceof MobEntity mob) { mob.setAiDisabled(true); mob.setPersistent(); }
        if (living instanceof PlayerShopkeeperEntity playerObject) applyPlayerSkin(shop, playerObject);
        living.setInvulnerable(true);
        living.setSilent(shop.silent);
        living.addCommandTag(ENTITY_TAG);
        living.addCommandTag("bd_shopkeeper_" + shop.id);
        living.setCustomName(shop.getDisplayName());
        living.setCustomNameVisible(true);
        if (!world.spawnEntity(living)) return null;
        shop.entityId = living.getUuid();
        captureObjectData(shop, living);
        return living;
    }

    public static boolean isPlayerObject(Shopkeeper shop) {
        return shop != null && "bd:player_shopkeeper".equals(shop.objectType);
    }

    public static void usePlayerProfile(Shopkeeper shop, GameProfile profile) {
        if (shop == null || profile == null) return;
        shop.skinName = profile.getName();
        shop.skinUuid = profile.getId();
        Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
        shop.skinTexture = textures == null ? "" : textures.value();
        shop.skinSignature = textures == null || !textures.hasSignature() ? "" : textures.signature();
    }

    /** Resolves a Mojang profile without blocking the server tick and updates the live object when ready. */
    public static boolean setPlayerSkin(MinecraftServer server, Shopkeeper shop, String playerName) {
        String name = playerName == null ? "" : playerName.trim();
        if (!isPlayerObject(shop) || !name.matches("[A-Za-z0-9_]{3,16}")) return false;
        shop.skinName = name;
        shop.skinUuid = null;
        shop.skinTexture = "";
        shop.skinSignature = "";
        updateLivePlayerSkin(server, shop);
        ShopkeeperManager.get(server).changed();

        server.getUserCache().findByNameAsync(name)
                .thenCompose(found -> found.<CompletableFuture<GameProfile>>map(profile ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                ProfileResult result = server.getSessionService().fetchProfile(profile.getId(), true);
                                return result == null ? profile : result.profile();
                            } catch (RuntimeException ignored) {
                                return profile;
                            }
                        })).orElseGet(() -> CompletableFuture.completedFuture(null)))
                .thenAccept(profile -> server.execute(() -> {
                    if (profile == null || !name.equalsIgnoreCase(shop.skinName)) return;
                    usePlayerProfile(shop, profile);
                    updateLivePlayerSkin(server, shop);
                    ShopkeeperManager.get(server).changed();
                }));
        return true;
    }

    private static void updateLivePlayerSkin(MinecraftServer server, Shopkeeper shop) {
        if (shop.entityId == null) return;
        for (ServerWorld candidate : server.getWorlds()) {
            Entity entity = candidate.getEntity(shop.entityId);
            if (entity instanceof PlayerShopkeeperEntity playerObject) {
                applyPlayerSkin(shop, playerObject);
                return;
            }
        }
    }

    private static void applyPlayerSkin(Shopkeeper shop, PlayerShopkeeperEntity entity) {
        entity.setSkinProfile(shop.skinName, shop.skinUuid, shop.skinTexture, shop.skinSignature);
    }

    public static void captureObjectData(Shopkeeper shop, Entity entity) {
        net.minecraft.nbt.NbtCompound data = new net.minecraft.nbt.NbtCompound();
        entity.writeNbt(data);
        for (String key : new String[]{"id", "UUID", "Pos", "Motion", "Rotation", "Dimension", "Passengers", "Leash",
                "Tags", "CustomName", "CustomNameVisible", "Invulnerable", "NoAI", "Health", "HurtTime", "DeathTime",
                "Fire", "Air", "FallDistance", "PortalCooldown"}) data.remove(key);
        shop.objectData = data;
    }

    public static void removeObject(MinecraftServer server, Shopkeeper shop) {
        if (shop.entityId == null) return;
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(shop.entityId);
            if (entity != null) { entity.discard(); return; }
        }
    }

    public static ServerWorld world(MinecraftServer server, String id) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(id)) return world;
        }
        return null;
    }

    public static Inventory inventoryAt(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof Inventory inventory ? inventory : null;
    }

    private static void recoverObjects(MinecraftServer server) {
        if (++recoveryTicks < 100) return;
        recoveryTicks = 0;
        ShopkeeperManager manager = ShopkeeperManager.get(server);
        boolean changed = false;
        for (Shopkeeper shop : manager.all()) {
            if (shop.signObject || shop.objectPos == null) continue;
            ServerWorld expectedWorld = world(server, shop.worldId);
            if (expectedWorld == null || !expectedWorld.isChunkLoaded(shop.objectPos)) continue;
            Entity entity = null;
            if (shop.entityId != null) {
                for (ServerWorld candidate : server.getWorlds()) {
                    entity = candidate.getEntity(shop.entityId);
                    if (entity != null) break;
                }
            }
            if (entity == null) {
                Identifier type = Identifier.tryParse(shop.objectType);
                if (type != null && spawnObject(expectedWorld, shop, type, Vec3d.ofBottomCenter(shop.objectPos)) != null) changed = true;
                continue;
            }
            entity.setInvulnerable(true);
            entity.setSilent(shop.silent);
            Text displayName = shop.getDisplayName();
            if (!displayName.equals(entity.getCustomName())) entity.setCustomName(displayName);
            entity.setCustomNameVisible(true);
            entity.addCommandTag(ENTITY_TAG);
            entity.addCommandTag("bd_shopkeeper_" + shop.id);
            if (entity instanceof MobEntity mob) { mob.setAiDisabled(true); mob.setPersistent(); }
            if (entity instanceof PlayerShopkeeperEntity playerObject) applyPlayerSkin(shop, playerObject);
            if (entity.getWorld() != expectedWorld || squaredDistance(entity.getPos(), shop.objectPos) > 2.25) {
                entity.teleport(expectedWorld, shop.objectPos.getX() + .5, shop.objectPos.getY(), shop.objectPos.getZ() + .5,
                        java.util.Set.of(), entity.getYaw(), entity.getPitch());
            }
        }
        if (changed) manager.changed();
    }

    private static double squaredDistance(Vec3d point, BlockPos pos) {
        return point.squaredDistanceTo(Vec3d.ofCenter(pos));
    }

    private static boolean shopAdmin(ServerPlayerEntity player) {
        return PermissionCompat.check(player, "shopkeeper.admin", player.hasPermissionLevel(2));
    }

    private ShopkeeperSystem() {}
}
