package com.zrollus.bd.shopkeeper;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;

import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

final class ShopkeeperMerchant implements Merchant {
    private final MinecraftServer server;
    private final Shopkeeper shop;
    private final ShopkeeperManager manager;
    private final TradeOfferList offers = new TradeOfferList();
    private final Map<TradeOffer, ShopkeeperTrade> tradeLookup = new IdentityHashMap<>();
    private PlayerEntity customer;

    ShopkeeperMerchant(MinecraftServer server, Shopkeeper shop) {
        this.server = server;
        this.shop = shop;
        this.manager = ShopkeeperManager.get(server);
        rebuildOffers();
    }

    private void rebuildOffers() {
        offers.clear();
        tradeLookup.clear();
        Inventory stock = inventory();
        for (ShopkeeperTrade trade : shop.trades) {
            if (trade.normalizeCosts()) manager.changed();
            if (trade.costOne.isEmpty() || trade.result.isEmpty()) continue;
            int available = shop.isAdmin() ? 999_999 : count(stock, trade.result, shop.exactItems) / trade.result.getCount();
            if (shop.type == Shopkeeper.Type.BOOK && !shop.isAdmin()) {
                boolean hasOriginal = available > 0;
                available = hasOriginal ? count(stock, Items.WRITABLE_BOOK.getDefaultStack(), false) : 0;
            }
            TradedItem first = tradedItem(trade.costOne);
            Optional<TradedItem> second = trade.costTwo.isEmpty()
                    ? Optional.empty()
                    : Optional.of(tradedItem(trade.costTwo));
            // Keep configured trades visible even without stock. A disabled
            // vanilla offer is rendered with the familiar red X and cannot be
            // selected for a completed purchase.
            TradeOffer offer = new TradeOffer(first, second, trade.result.copy(), 0, Math.max(1, available), 0, 0.0F);
            if (available <= 0) offer.disable();
            offers.add(offer);
            tradeLookup.put(offer, trade);
        }
    }

    private TradedItem tradedItem(ItemStack stack) {
        if (!shop.exactItems) return new TradedItem(stack.getItem(), stack.getCount());
        return new TradedItem(stack.getItem().getRegistryEntry(), stack.getCount(),
                ComponentPredicate.of(stack.getComponents()), stack.copy());
    }

    @Override public void setCustomer(PlayerEntity customer) { this.customer = customer; }
    @Override public PlayerEntity getCustomer() { return customer; }
    @Override public TradeOfferList getOffers() { return offers; }
    @Override public void setOffersFromServer(TradeOfferList offers) {}

    @Override
    public void sendOffers(PlayerEntity player, Text name, int levelProgress) {
        OptionalInt syncId = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (id, inventory, ignored) -> new ShopkeeperMerchantScreenHandler(id, inventory, this), name));
        if (syncId.isPresent() && !offers.isEmpty()) {
            player.sendTradeOffers(syncId.getAsInt(), offers, levelProgress, getExperience(),
                    isLeveledMerchant(), canRefreshTrades());
        }
    }

    @Override
    public void trade(TradeOffer offer) {
        ShopkeeperTrade trade = tradeLookup.get(offer);
        if (trade == null || !(customer instanceof ServerPlayerEntity buyer)) return;
        Inventory stock = inventory();

        if (!shop.isAdmin()) {
            List<ItemStack> before = snapshot(stock);
            boolean stockTaken;
            if (shop.type == Shopkeeper.Type.BOOK) {
                stockTaken = count(stock, trade.result, shop.exactItems) >= trade.result.getCount()
                        && remove(stock, Items.WRITABLE_BOOK.getDefaultStack(), 1, false);
            } else {
                stockTaken = remove(stock, trade.result, trade.result.getCount(), shop.exactItems);
            }
            if (!stockTaken || !add(stock, trade.costOne.copy())
                    || (!trade.costTwo.isEmpty() && !add(stock, trade.costTwo.copy()))) {
                restore(stock, before);
                buyer.sendMessage(Text.literal("Shopkeeper stock changed; trade cancelled.").formatted(Formatting.RED));
                refundAndRemoveResult(buyer, trade);
                rebuildOffers();
                return;
            }
            stock.markDirty();
        }

        offer.use();
        if (shop.isAdmin()) offer.resetUses();
        String buyerName = buyer.getName().getString();
        String line = Instant.now() + " | " + shop.id + " | " + buyerName + " traded "
                + describe(trade.costOne) + (trade.costTwo.isEmpty() ? "" : " + " + describe(trade.costTwo))
                + " for " + describe(trade.result);
        manager.log(line);

        if (shop.notifyOwner && shop.owner != null) {
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(shop.owner);
            if (owner != null && owner != buyer) owner.sendMessage(Text.literal("[Shop] " + buyerName + " bought " + describe(trade.result)).formatted(Formatting.GREEN));
        }
        if (shop.notifyOwner) {
            for (var member : shop.members.keySet()) {
                ServerPlayerEntity memberPlayer = server.getPlayerManager().getPlayer(member);
                if (memberPlayer != null && memberPlayer != buyer)
                    memberPlayer.sendMessage(Text.literal("[Shop] " + buyerName + " bought " + describe(trade.result)).formatted(Formatting.GREEN));
            }
        }
        if (!trade.command.isBlank()) {
            String command = trade.command
                    .replace("{player}", buyerName)
                    .replace("{uuid}", buyer.getUuidAsString())
                    .replace("{player_name}", buyerName)
                    .replace("{player_uuid}", buyer.getUuidAsString())
                    .replace("{player_displayname}", buyer.getDisplayName().getString())
                    .replace("{shop_uuid}", shop.id.toString());
            int commands = reclaimResult(buyer, trade.result);
            if (commands == trade.result.getCount()) {
                for (int i = 0; i < commands; i++)
                    server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), command);
            } else {
                if (commands > 0) buyer.getInventory().offerOrDrop(trade.result.copyWithCount(commands));
                buyer.sendMessage(Text.literal("The traded command could not safely consume its trigger item; no command ran.").formatted(Formatting.RED));
            }
        }
        manager.changed();
    }

    private void refundAndRemoveResult(ServerPlayerEntity buyer, ShopkeeperTrade trade) {
        int remaining = trade.result.getCount() - reclaimResult(buyer, trade.result);
        // Refund only after reclaiming the result. Otherwise a stock race could
        // grant both the product and its payment back.
        if (remaining == 0) {
            buyer.getInventory().offerOrDrop(trade.costOne.copy());
            if (!trade.costTwo.isEmpty()) buyer.getInventory().offerOrDrop(trade.costTwo.copy());
        }
    }

    private int reclaimResult(ServerPlayerEntity buyer, ItemStack result) {
        int remaining = result.getCount();
        ItemStack cursor = buyer.currentScreenHandler.getCursorStack();
        if (matches(cursor, result, true)) {
            int take = Math.min(remaining, cursor.getCount());
            cursor.decrement(take);
            remaining -= take;
        }
        if (remaining > 0) remaining -= removeAmount(buyer.getInventory(), result, remaining, true);
        return result.getCount() - remaining;
    }

    private static int removeAmount(Inventory inventory, ItemStack template, int amount, boolean exact) {
        int removed = 0;
        for (int i = 0; i < inventory.size() && removed < amount; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!matches(stack, template, exact)) continue;
            int take = Math.min(amount - removed, stack.getCount());
            stack.decrement(take);
            removed += take;
        }
        if (removed > 0) inventory.markDirty();
        return removed;
    }

    private Inventory inventory() {
        if (shop.containerPos == null) return null;
        ServerWorld world = ShopkeeperSystem.world(server, shop.containerWorldId);
        if (world == null) return null;
        BlockEntity blockEntity = world.getBlockEntity(shop.containerPos);
        return blockEntity instanceof Inventory inventory ? inventory : null;
    }

    static int count(Inventory inventory, ItemStack template, boolean exact) {
        if (inventory == null) return 0;
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (matches(stack, template, exact)) count += stack.getCount();
        }
        return count;
    }

    static boolean remove(Inventory inventory, ItemStack template, int amount, boolean exact) {
        if (inventory == null || count(inventory, template, exact) < amount) return false;
        for (int i = 0; i < inventory.size() && amount > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (matches(stack, template, exact)) {
                int take = Math.min(amount, stack.getCount());
                stack.decrement(take);
                amount -= take;
            }
        }
        inventory.markDirty();
        return true;
    }

    static boolean add(Inventory inventory, ItemStack added) {
        if (inventory == null) return false;
        ItemStack remaining = added.copy();
        for (int i = 0; i < inventory.size() && !remaining.isEmpty(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, remaining)) {
                int move = Math.min(remaining.getCount(), stack.getMaxCount() - stack.getCount());
                stack.increment(move);
                remaining.decrement(move);
            }
        }
        for (int i = 0; i < inventory.size() && !remaining.isEmpty(); i++) {
            if (inventory.getStack(i).isEmpty()) {
                int move = Math.min(remaining.getCount(), remaining.getMaxCount());
                inventory.setStack(i, remaining.copyWithCount(move));
                remaining.decrement(move);
            }
        }
        inventory.markDirty();
        return remaining.isEmpty();
    }

    private static List<ItemStack> snapshot(Inventory inventory) {
        List<ItemStack> contents = new ArrayList<>(inventory.size());
        for (int i = 0; i < inventory.size(); i++) contents.add(inventory.getStack(i).copy());
        return contents;
    }

    private static void restore(Inventory inventory, List<ItemStack> contents) {
        for (int i = 0; i < inventory.size(); i++) inventory.setStack(i, contents.get(i).copy());
        inventory.markDirty();
    }

    private static boolean matches(ItemStack stack, ItemStack template, boolean exact) {
        return !stack.isEmpty() && (exact
                ? ItemStack.areItemsAndComponentsEqual(stack, template)
                : ItemStack.areItemsEqual(stack, template));
    }

    static String describe(ItemStack stack) {
        return stack.getCount() + "x " + stack.getName().getString();
    }

    @Override public void onSellingItem(ItemStack stack) {}
    @Override public int getExperience() { return 0; }
    @Override public void setExperienceFromServer(int experience) {}
    @Override public boolean isLeveledMerchant() { return false; }
    @Override public SoundEvent getYesSound() { return SoundEvents.ENTITY_VILLAGER_YES; }
    @Override public boolean canRefreshTrades() { return false; }
    @Override public boolean isClient() { return false; }
}
