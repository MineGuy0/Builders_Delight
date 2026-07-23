package com.zrollus.bd.GUI;

import com.zrollus.bd.Lib.ModConfigHelper;
import com.zrollus.bd.Lib.PermissionCompat;
import com.zrollus.bd.Entity.PlayerShopkeeperEntity;
import com.zrollus.bd.shopkeeper.Shopkeeper;
import com.zrollus.bd.shopkeeper.ShopkeeperManager;
import com.zrollus.bd.shopkeeper.ShopkeeperMobVariants;
import com.zrollus.bd.shopkeeper.ShopkeeperPlaceholderItems;
import com.zrollus.bd.shopkeeper.ShopkeeperSystem;
import com.zrollus.bd.shopkeeper.ShopkeeperTrade;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


/** Shopkeepers-style editor: eight vertical ghost-slot trades per page plus an option row. */
public final class ShopkeeperEditorScreenHandler extends ScreenHandler {
    private static final int TRADES_PER_PAGE = 9;
    private static final int MENU_SIZE = 54;
    private final Inventory menu;
    private final Shopkeeper shop;
    private int page;
    private boolean deleteArmed;
    private final Runnable closeAction;
    private final Property previewEntityId = Property.create();
    private final Property pageNumber = Property.create();
    private final Property pageCount = Property.create();
    private final Property closedState = Property.create();
    private final Property silentState = Property.create();

    public ShopkeeperEditorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null, null);
    }

    public ShopkeeperEditorScreenHandler(int syncId, PlayerInventory playerInventory, Shopkeeper shop) {
        this(syncId, playerInventory, shop, null);
    }

    public ShopkeeperEditorScreenHandler(int syncId, PlayerInventory playerInventory, Shopkeeper shop, Runnable closeAction) {
        super(ModScreenHandlers.SHOPKEEPER_EDITOR, syncId);
        this.menu = new SimpleInventory(MENU_SIZE);
        this.shop = shop;
        this.closeAction = closeAction;
        checkSize(menu, MENU_SIZE);
        menu.onOpen(playerInventory.player);

        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(menu, column + row * 9, 5 + column * 19, 17 + row * 19));
        for (int index = 27; index < MENU_SIZE; index++)
            addSlot(new Slot(menu, index, -10_000, -10_000));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(playerInventory, column + (row + 1) * 9, 5 + column * 19, 91 + row * 19));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, 5 + column * 19, 148));

        addProperty(previewEntityId);
        addProperty(pageNumber);
        addProperty(pageCount);
        addProperty(closedState);
        addProperty(silentState);
        if (shop != null && playerInventory.player instanceof ServerPlayerEntity serverPlayer) {
            Entity object = findEntity(serverPlayer, shop.entityId);
            previewEntityId.set(object == null || object.getWorld() != serverPlayer.getWorld() ? -1 : object.getId());
            refresh();
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        menu.onClose(player);
        if (closeAction != null) closeAction.run();
    }

    public int getPreviewEntityId() { return previewEntityId.get(); }
    public int getPageNumber() { return pageNumber.get(); }
    public int getPageCount() { return Math.max(1, pageCount.get()); }
    public boolean isShopClosed() { return closedState.get() != 0; }
    public boolean isShopSilent() { return silentState.get() != 0; }

    @Override
    public boolean canUse(PlayerEntity player) {
        boolean admin = player instanceof ServerPlayerEntity serverPlayer
                && PermissionCompat.check(serverPlayer, "shopkeeper.admin", serverPlayer.hasPermissionLevel(2));
        return shop == null || shop.canManage(player.getUuid(), admin);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < MENU_SIZE) {
            if (player instanceof ServerPlayerEntity serverPlayer && shop != null) handleMenuClick(slotIndex, button, actionType, serverPlayer);
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    private void handleMenuClick(int slot, int button, SlotActionType actionType, ServerPlayerEntity player) {
        int row = slot / 9;
        int column = slot % 9;

        // The top three rows are result, cost one and cost two ghost slots.
        if (row <= 2 && column < TRADES_PER_PAGE) {
            setTradeGhost(page * TRADES_PER_PAGE + column, row, getCursorStack(), player);
            return;
        }

        if (slot == 27 && page > 0) {
            page--;
            deleteArmed = false;
            refresh();
        } else if (slot == 35 && page < maxPage()) {
            page++;
            deleteArmed = false;
            refresh();
        } else if (slot == 28 || slot == 29 || slot == 30 || slot == 32 || slot == 33 || slot == 34) {
            cycleVariantControl(slot, player);
        } else if (slot == 36) {
            Text customName = getCursorStack().get(DataComponentTypes.CUSTOM_NAME);
            if (getCursorStack().isOf(Items.NAME_TAG) && customName != null) {
                shop.setName(customName);
                Entity entity = findEntity(player, shop.entityId);
                if (entity != null) entity.setCustomName(shop.getDisplayName());
                if (ShopkeeperSystem.isPlayerObject(shop))
                    ShopkeeperSystem.setPlayerSkin(player.getServer(), shop, shop.name);
                changed(player);
            } else {
                player.sendMessage(Text.literal("Hold a renamed name tag on your cursor, then click this option.").formatted(Formatting.YELLOW), true);
            }
        } else if (slot == 37) {
            Entity entity = findEntity(player, shop.entityId);
            if (entity instanceof PlayerShopkeeperEntity) {
                Text requestedName = getCursorStack().get(DataComponentTypes.CUSTOM_NAME);
                if (requestedName != null && ShopkeeperSystem.setPlayerSkin(player.getServer(), shop, requestedName.getString())) {
                    player.sendMessage(Text.literal("Player skin set to " + requestedName.getString() + "; it will update after lookup.").formatted(Formatting.GREEN), true);
                    changed(player);
                } else {
                    player.sendMessage(Text.literal("Hold an item renamed to a Minecraft player name on your cursor, then click Change Skin.").formatted(Formatting.YELLOW), true);
                }
            } else if (entity instanceof VillagerEntity villager) {
                if (actionType == SlotActionType.QUICK_MOVE && button == 0) {
                    int level = villager.getVillagerData().getLevel();
                    villager.setVillagerData(villager.getVillagerData().withLevel(level >= 5 ? 1 : level + 1));
                } else if (actionType == SlotActionType.QUICK_MOVE && button == 1) {
                    villager.setBaby(!villager.isBaby());
                } else if (button == 1) {
                    var values = net.minecraft.registry.Registries.VILLAGER_TYPE.stream().toList();
                    int current = values.indexOf(villager.getVillagerData().getType());
                    villager.setVillagerData(villager.getVillagerData().withType(values.get((current + 1) % values.size())));
                } else {
                    var values = net.minecraft.registry.Registries.VILLAGER_PROFESSION.stream().toList();
                    int current = values.indexOf(villager.getVillagerData().getProfession());
                    villager.setVillagerData(villager.getVillagerData().withProfession(values.get((current + 1) % values.size())));
                }
                ShopkeeperSystem.captureObjectData(shop, villager);
                changed(player);
            } else if (entity instanceof LivingEntity) {
                String result = ShopkeeperMobVariants.cycle(entity, button == 1, actionType == SlotActionType.QUICK_MOVE);
                ShopkeeperSystem.captureObjectData(shop, entity);
                player.sendMessage(Text.literal(result).formatted(Formatting.AQUA), true);
                changed(player);
            } else {
                player.sendMessage(Text.literal("This shopkeeper object has no selectable mob variants.").formatted(Formatting.YELLOW), true);
            }
        } else if (slot == 38) {
            shop.closed = !shop.closed;
            changed(player);
        } else if (slot == 39) {
            Entity entity = findEntity(player, shop.entityId);
            if (entity instanceof LivingEntity living) {
                for (EquipmentSlot equipmentSlot : EquipmentSlot.values())
                    living.equipStack(equipmentSlot, player.getEquippedStack(equipmentSlot).copy());
                ShopkeeperSystem.captureObjectData(shop, living);
                player.sendMessage(Text.literal("Shopkeeper equipment updated.").formatted(Formatting.GREEN), true);
            }
        } else if (slot == 40) {
            openStock(player);
        } else if (slot == 41) {
            shop.notifyOwner = !shop.notifyOwner;
            changed(player);
        } else if (slot == 42) {
            ShopkeeperSystem.beginMove(player, shop);
        } else if (slot == 43) {
            if (!shop.canFullyManage(player.getUuid(), PermissionCompat.check(player, "shopkeeper.admin", player.hasPermissionLevel(2)))) {
                player.sendMessage(Text.literal("Only the owner or a full-access member can manage members.").formatted(Formatting.RED), true);
            } else {
                player.sendMessage(Text.literal("Members: /sk members add|level|remove " + shop.id + " <player> [container|edit|full]").formatted(Formatting.AQUA));
            }
        } else if (slot == 44) {
            if (!shop.canFullyManage(player.getUuid(), PermissionCompat.check(player, "shopkeeper.admin", player.hasPermissionLevel(2)))) {
                player.sendMessage(Text.literal("Only the owner or a full-access member can delete this shop.").formatted(Formatting.RED), true);
                return;
            }
            if (!deleteArmed) {
                deleteArmed = true;
                refresh();
                player.sendMessage(Text.literal("Click the bone again to permanently delete this shopkeeper.").formatted(Formatting.RED), true);
            } else {
                ShopkeeperSystem.removeObject(player.getServer(), shop);
                ShopkeeperManager.get(player.getServer()).remove(shop.id);
                player.closeHandledScreen();
                player.sendMessage(Text.literal("Shopkeeper deleted. Stock contents were preserved.").formatted(Formatting.RED));
            }
        } else if (slot == 45) {
            shop.silent = !shop.silent;
            Entity entity = findEntity(player, shop.entityId);
            if (entity != null) entity.setSilent(shop.silent);
            changed(player);
        }
    }

    private void cycleVariantControl(int slot, ServerPlayerEntity player) {
        Entity entity = findEntity(player, shop.entityId);
        if (!(entity instanceof LivingEntity living)) {
            player.sendMessage(Text.literal("This shop object has no living-mob variants.").formatted(Formatting.YELLOW), true);
            return;
        }
        if (living instanceof VillagerEntity villager) {
            if (slot == 28 || slot == 29) {
                var values = net.minecraft.registry.Registries.VILLAGER_PROFESSION.stream().toList();
                int current = values.indexOf(villager.getVillagerData().getProfession());
                villager.setVillagerData(villager.getVillagerData().withProfession(
                        values.get(Math.floorMod(current + (slot == 29 ? -1 : 1), values.size()))));
            } else if (slot == 30 || slot == 32) {
                var values = net.minecraft.registry.Registries.VILLAGER_TYPE.stream().toList();
                int current = values.indexOf(villager.getVillagerData().getType());
                villager.setVillagerData(villager.getVillagerData().withType(
                        values.get(Math.floorMod(current + (slot == 32 ? -1 : 1), values.size()))));
            } else if (slot == 33) {
                int level = villager.getVillagerData().getLevel();
                villager.setVillagerData(villager.getVillagerData().withLevel(level >= 5 ? 1 : level + 1));
            } else {
                villager.setBaby(!villager.isBaby());
            }
            player.sendMessage(Text.literal("Villager variant updated.").formatted(Formatting.AQUA), true);
        } else {
            int channel = switch (slot) {
                case 28, 29 -> 0;
                case 30, 32 -> 1;
                case 33 -> 3;
                default -> 2;
            };
            boolean backwards = slot == 29 || slot == 32;
            player.sendMessage(Text.literal(ShopkeeperMobVariants.cycleChannel(entity, channel, backwards))
                    .formatted(Formatting.AQUA), true);
        }
        ShopkeeperSystem.captureObjectData(shop, living);
        changed(player);
    }

    private void setTradeGhost(int index, int row, ItemStack cursor, ServerPlayerEntity player) {
        if (index >= ModConfigHelper.get().maxShopTradePages * TRADES_PER_PAGE) {
            player.sendMessage(Text.literal("This shop reached its configured trade-page limit.").formatted(Formatting.RED), true);
            return;
        }
        while (shop.trades.size() <= index) shop.trades.add(new ShopkeeperTrade());
        ShopkeeperTrade trade = shop.trades.get(index);
        ItemStack ghost = cursor.isEmpty() ? ItemStack.EMPTY
                : ShopkeeperPlaceholderItems.replace(cursor, shop.isAdmin());
        if (row == 0) trade.result = ghost;
        else if (row == 1) trade.costOne = ghost;
        else trade.costTwo = ghost;
        trade.normalizeCosts();

        while (!shop.trades.isEmpty()) {
            ShopkeeperTrade last = shop.trades.getLast();
            if (!last.result.isEmpty() || !last.costOne.isEmpty() || !last.costTwo.isEmpty()) break;
            shop.trades.removeLast();
        }
        changed(player);
    }

    private void openStock(ServerPlayerEntity player) {
        if (shop.containerPos == null) {
            player.sendMessage(Text.literal("Admin shops do not use a stock container.").formatted(Formatting.YELLOW), true);
            return;
        }
        ServerWorld world = ShopkeeperSystem.world(player.getServer(), shop.containerWorldId);
        BlockEntity blockEntity = world == null ? null : world.getBlockEntity(shop.containerPos);
        if (blockEntity instanceof NamedScreenHandlerFactory factory) player.openHandledScreen(factory);
        else player.sendMessage(Text.literal("The stock container is missing.").formatted(Formatting.RED), true);
    }

    private Entity findEntity(ServerPlayerEntity player, java.util.UUID uuid) {
        if (uuid == null) return null;
        for (ServerWorld world : player.getServer().getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    private void changed(ServerPlayerEntity player) {
        ShopkeeperManager.get(player.getServer()).changed();
        refresh();
    }

    private void refresh() {
        if (shop == null) return;
        menu.clear();
        int start = page * TRADES_PER_PAGE;
        for (int column = 0; column < TRADES_PER_PAGE; column++) {
            int index = start + column;
            ShopkeeperTrade trade = index < shop.trades.size() ? shop.trades.get(index) : null;
            if (trade != null) trade.normalizeCosts();
            menu.setStack(column, displayOrPlaceholder(trade == null ? ItemStack.EMPTY : trade.result, "Sold"));
            menu.setStack(9 + column, displayOrPlaceholder(trade == null ? ItemStack.EMPTY : trade.costOne, "Cost 1"));
            menu.setStack(18 + column, displayOrPlaceholder(trade == null ? ItemStack.EMPTY : trade.costTwo, "Cost 2"));
        }
        if (page > 0) menu.setStack(27, named(Items.WRITABLE_BOOK.getDefaultStack(), "Previous page"));
        if (!shop.signObject) {
            menu.setStack(28, named(Items.MAGENTA_DYE.getDefaultStack(), "Next appearance / profession"));
            menu.setStack(29, named(Items.PURPLE_DYE.getDefaultStack(), "Previous appearance / profession"));
            menu.setStack(30, named(Items.CYAN_DYE.getDefaultStack(), "Next secondary variant / biome"));
            menu.setStack(32, named(Items.BLUE_DYE.getDefaultStack(), "Previous secondary variant / biome"));
            menu.setStack(33, named(Items.EGG.getDefaultStack(), "Baby/adult (villagers: level)"));
            menu.setStack(34, named(Items.GLOW_INK_SAC.getDefaultStack(), "Tertiary variant (villagers: baby/adult)"));
        }
        menu.setStack(31, named(Items.PAPER.getDefaultStack(), "Page " + (page + 1) + " / " + (maxPage() + 1)));
        if (page < maxPage()) menu.setStack(35, named(Items.WRITABLE_BOOK.getDefaultStack(), "Next page"));

        menu.setStack(36, named(Items.NAME_TAG.getDefaultStack(), "Rename (use a renamed name tag)"));
        if (ShopkeeperSystem.isPlayerObject(shop)) {
            menu.setStack(37, named(Items.PLAYER_HEAD.getDefaultStack(),
                    "Change Skin: " + (shop.skinName.isEmpty() ? "Steve" : shop.skinName) + " (use a renamed item)"));
        } else {
            String variantHelp = "minecraft:villager".equals(shop.objectType)
                    ? "left profession, right biome, shift-left level, shift-right baby"
                    : "left appearance, right baby/secondary, shift reverses";
            menu.setStack(37, named(Items.VILLAGER_SPAWN_EGG.getDefaultStack(),
                    "Object: " + shop.objectType + " (" + variantHelp + ")"));
        }
        menu.setStack(38, named((shop.closed ? Items.RED_BANNER : Items.GREEN_BANNER).getDefaultStack(), shop.closed ? "Open shop" : "Close shop"));
        menu.setStack(39, named(Items.LEATHER_CHESTPLATE.getDefaultStack(), "Copy your equipment to shopkeeper"));
        menu.setStack(40, shop.isAdmin()
                ? named(Items.NETHER_STAR.getDefaultStack(), "Admin shop: unlimited stock")
                : named(Items.CHEST.getDefaultStack(), "Open stock container"));
        menu.setStack(41, named(Items.BELL.getDefaultStack(), "Trade notifications: " + (shop.notifyOwner ? "on" : "off")));
        menu.setStack(42, named(Items.ENDER_PEARL.getDefaultStack(), "Move: right-click the block it should stand above"));
        menu.setStack(43, named(Items.PLAYER_HEAD.getDefaultStack(), "Manage members"));
        menu.setStack(44, named(Items.BONE.getDefaultStack(), deleteArmed ? "Click again: DELETE" : "Delete shopkeeper"));
        pageNumber.set(page + 1);
        pageCount.set(maxPage() + 1);
        closedState.set(shop.closed ? 1 : 0);
        silentState.set(shop.silent ? 1 : 0);
        sendContentUpdates();
    }

    private int maxPage() {
        int accessible = shop.trades.size() / TRADES_PER_PAGE;
        return Math.min(ModConfigHelper.get().maxShopTradePages - 1, accessible);
    }

    private static ItemStack displayOrPlaceholder(ItemStack stack, String name) {
        if (!stack.isEmpty()) return stack.copy();
        ItemStack placeholder = switch (name) {
            case "Sold" -> Items.LIME_STAINED_GLASS_PANE.getDefaultStack();
            case "Cost 1" -> Items.RED_STAINED_GLASS_PANE.getDefaultStack();
            default -> Items.ORANGE_STAINED_GLASS_PANE.getDefaultStack();
        };
        return named(placeholder, "Set " + name);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.GOLD));
        return stack;
    }
}
