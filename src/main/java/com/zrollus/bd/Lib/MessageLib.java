package com.zrollus.bd.Lib;

import com.zrollus.bd.Lib.libHelpers.ShopSignData;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MessageLib {

    // --- PREFIXES & THEMES ---
    public static final Formatting COLOR_MAIN = Formatting.GOLD;
    public static final Formatting COLOR_TEXT = Formatting.YELLOW;
    public static final Formatting COLOR_ERROR = Formatting.RED;
    public static final Formatting COLOR_ACCENT = Formatting.WHITE;

    // --- TPA MESSAGES ---
    public static Text tpaHereRequestReceived(String senderName) {
        return Text.literal("")
                .append(Text.literal(senderName).formatted(COLOR_MAIN))
                .append(Text.literal(" has requested that you teleport to them.").formatted(COLOR_TEXT))
                .append(Text.literal("\nTo teleport, type ").formatted(COLOR_TEXT))
                .append(Text.literal("/tpaccept").formatted(COLOR_MAIN))
                .append(Text.literal(" or ").formatted(COLOR_TEXT))
                .append(Text.literal("/tpdeny").formatted(COLOR_MAIN))
                .append(Text.literal(". ").formatted(COLOR_TEXT))
                .append(getTpaButtons());
    }

    public static Text msgSend(String targetName, String msg) {
        return Text.literal("[").formatted(Formatting.GOLD)
                .append(Text.literal("me").formatted(Formatting.RED))
                .append(Text.literal(" -> ").formatted(Formatting.GOLD))
                .append(Text.literal(targetName).formatted(Formatting.RED))
                .append(Text.literal("] ").formatted(Formatting.GOLD))
                .append(Text.literal(msg).formatted(Formatting.WHITE));
    }

    public static Text msgReceive(String senderName, String msg) {
        return Text.literal("[").formatted(Formatting.GOLD)
                .append(Text.literal(senderName).formatted(Formatting.RED))
                .append(Text.literal(" -> ").formatted(Formatting.GOLD))
                .append(Text.literal("you").formatted(Formatting.RED))
                .append(Text.literal("] ").formatted(Formatting.GOLD))
                .append(Text.literal(msg).formatted(Formatting.WHITE));
    }

    public static Text tpaHereRequestSent(String targetName) {
        return Text.literal("Request sent to ").formatted(COLOR_TEXT)
                .append(Text.literal(targetName).formatted(COLOR_MAIN));
    }

    public static Text tpaRequestReceived(String senderName) {
        return Text.literal("")
                .append(Text.literal(senderName).formatted(COLOR_MAIN))
                .append(Text.literal(" has requested to teleport to you.").formatted(COLOR_TEXT))
                .append(Text.literal("\nTo accept, type ").formatted(COLOR_TEXT))
                .append(Text.literal("/tpaccept").formatted(COLOR_MAIN))
                .append(Text.literal(" or ").formatted(COLOR_TEXT))
                .append(Text.literal("/tpdeny").formatted(COLOR_MAIN))
                .append(Text.literal(". ").formatted(COLOR_TEXT))
                .append(getTpaButtons());
    }

    public static Text tpaRequestSent(String targetName) {
        return Text.literal("Request sent to ").formatted(COLOR_TEXT)
                .append(Text.literal(targetName).formatted(COLOR_MAIN));
    }



    public static final Text TPA_TIMEOUT = Text.literal("Teleport request timed out.").formatted(Formatting.DARK_RED);
    public static final Text NO_PENDING_REQUEST = Text.literal("You do not have a pending request.").formatted(COLOR_ERROR);

    // --- HOME & WARP PLACEHOLDERS (For your future updates) ---
    public static Text homeSet(String name) {
        return Text.literal("Home ").formatted(COLOR_TEXT)
                .append(Text.literal(name).formatted(COLOR_MAIN))
                .append(Text.literal(" set.").formatted(COLOR_TEXT));
    }

    // --- HELPER METHODS ---
    private static MutableText getTpaButtons() {
        MutableText accept = Text.literal("[ACCEPT]")
                .styled(s -> s.withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to accept"))));

        MutableText deny = Text.literal(" [DENY]")
                .styled(s -> s.withColor(Formatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to deny"))));
        return Text.literal("\n").append(accept).append(deny);
    }

    // --- SHOP MESSAGES ---
    public static final Text SHOP_PREFIX = Text.literal("Shop » ").formatted(COLOR_MAIN);

    public static Text shopError(String message) {
        return Text.literal("").append(SHOP_PREFIX).append(Text.literal(message).formatted(COLOR_ERROR));
    }

    public static Text shopSuccess(String action, int amount, String itemName, long price) {
        return Text.literal("").append(SHOP_PREFIX)
                .append(Text.literal("You " + action + " ").formatted(COLOR_ACCENT))
                .append(Text.literal(amount + "x ").formatted(COLOR_TEXT))
                .append(Text.literal(itemName).formatted(COLOR_TEXT))
                .append(Text.literal(" for ").formatted(COLOR_ACCENT))
                .append(Text.literal("₱" + price).formatted(Formatting.GREEN));
    }

    public static Text shopOutofMoney(long needed) {
        return shopError("You need ₱" + needed + " more!");
    }

    public static final Text SHOP_OWNER_SELF = Text.literal("Can't trade with your own shop, dingus!").formatted(COLOR_ERROR);
    public static final Text SHOP_OUT_OF_STOCK = shopError("This shop is out of stock!");
    public static final Text SHOP_CHEST_FULL = shopError("This shop's chest is full!");
    public static final Text SHOP_OWNER_BROKE = shopError("The shop owner is out of money!");

    // --- HOME & WARP MESSAGES ---
    public static Text homeLimitReached(int limit) {
        return Text.literal("You have reached your home limit of ").formatted(COLOR_ERROR)
                .append(Text.literal(String.valueOf(limit)).formatted(COLOR_MAIN))
                .append(Text.literal(".").formatted(COLOR_ERROR));
    }

    public static Text homeNotFound(String name) {
        return Text.literal("Home ").formatted(COLOR_ERROR)
                .append(Text.literal(name).formatted(COLOR_MAIN))
                .append(Text.literal(" not found.").formatted(COLOR_ERROR));
    }

    public static Text warpNotFound(String name) {
        return Text.literal("Warp ").formatted(COLOR_ERROR)
                .append(Text.literal(name).formatted(COLOR_MAIN))
                .append(Text.literal(" not found.").formatted(COLOR_ERROR));
    }

    public static Text homeDeleted(String name) {
        return Text.literal("Home ").formatted(COLOR_TEXT)
                .append(Text.literal(name).formatted(COLOR_MAIN))
                .append(Text.literal(" has been removed.").formatted(COLOR_TEXT));
    }

    // --- DEATH MESSAGES ---
    public static Text deathLocation(int x, int y, int z) {
        return Text.literal("You died at ").formatted(Formatting.GRAY)
                .append(Text.literal("X:" + x + " Y:" + y + " Z:" + z).formatted(COLOR_MAIN))
                .append(Text.literal(". Type ").formatted(Formatting.GRAY))
                .append(Text.literal("/back").formatted(COLOR_MAIN))
                .append(Text.literal(" to return.").formatted(Formatting.GRAY));
    }

    // --- MAIL NOTIFICATION (Updated for Join Event) ---
    public static Text mailJoinNotification(int count) {
        return ItemNameCommand.parseLegacyFormatting("&6&lMail &r&7» ").append(Text.literal("You have " + count + " unread messages!").formatted(COLOR_TEXT))
                .append(Text.literal(" Type ").formatted(COLOR_TEXT))
                .append(Text.literal("/mail read").formatted(COLOR_MAIN))
                .append(Text.literal(" to see them.").formatted(COLOR_TEXT));
    }

    // --- HOME & BACK MESSAGES ---
    public static final Text BACK_NO_LOCATION = Text.literal("You have no location to go back to.").formatted(COLOR_ERROR);
    public static final Text BACK_SUCCESS = Text.literal("Returning to previous location.").formatted(COLOR_MAIN);

    // This handles the "home_1", "home_2" styling specifically
    public static Text homeSetDefault(String name) {
        return Text.literal("new home: ").formatted(COLOR_TEXT)
                .append(Text.literal(name).formatted(COLOR_MAIN))
                .append(Text.literal(" has been set.").formatted(COLOR_TEXT));
    }

    // --- BALTOP & ECONOMY ---
    public static Text baltopHeader(String timestamp, int page, int maxPages) {
        return Text.literal(" ---- ").formatted(COLOR_TEXT)
                .append(Text.literal("Balancetop").formatted(COLOR_MAIN))
                .append(Text.literal(" -- ").formatted(COLOR_TEXT))
                .append(Text.literal("Page ").formatted(COLOR_MAIN))
                .append(Text.literal(String.valueOf(page)).formatted(COLOR_ERROR))
                .append(Text.literal("/").formatted(COLOR_MAIN))
                .append(Text.literal(String.valueOf(maxPages)).formatted(COLOR_ERROR))
                .append(Text.literal(" ---- ").formatted(COLOR_TEXT));
    }

    public static Text serverTotal(long total) {
        return Text.literal("Server Total: ").formatted(COLOR_MAIN)
                .append(Text.literal("₱" + String.format("%,d", total)).formatted(COLOR_ERROR));
    }

    // --- WARPS ---
    public static Text warpSet(String name) {
        return Text.literal("Warp '").formatted(COLOR_TEXT)
                .append(Text.literal(name).formatted(COLOR_MAIN))
                .append(Text.literal("' has been set!").formatted(COLOR_TEXT));
    }

    public static Text warpingTo(String name) {
        return Text.literal("Warping to ").formatted(COLOR_TEXT)
                .append(Text.literal(name).formatted(COLOR_MAIN));
    }

    // --- ITEM ID ---
    public static Text idMessage(String fullName, int id) {
        return Text.literal("Item: ").formatted(Formatting.GRAY)
                .append(Text.literal(fullName).formatted(COLOR_TEXT))
                .append(Text.literal(" | ID: ").formatted(Formatting.GRAY))
                .append(Text.literal("#" + id).formatted(Formatting.AQUA).formatted(Formatting.BOLD));
    }

    // --- TPA TOGGLE ---
    public static Text tpaStatus(boolean enabled) {
        Text status = enabled
                ? Text.literal("enabled").formatted(Formatting.GREEN)
                : Text.literal("disabled").formatted(COLOR_ERROR);
        return Text.literal("Teleportation requests are now ").formatted(COLOR_TEXT).append(status);
    }

    private static String formatName(String fullName) {
        String path = fullName.contains(":") ? fullName.split(":")[1] : fullName;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static void sendShopInfo(ServerPlayerEntity player, ShopSignData shop, Inventory chest) {
        System.out.println(chest);
        int stock = (chest == null) ? 0 : countStock(chest, shop.fullItemName.substring(shop.fullItemName.indexOf(':')+1));
        String itemName = formatName(shop.fullItemName);

        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&aShop Information:"), false);
        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&fOwner: &7" + shop.owner), false);
        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&fStock: &7" + stock), false);
        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&fItem: &7" + itemName), false);
        player.sendMessage(Text.literal(""), false); // Spacer
        player.sendMessage(ItemNameCommand.parseLegacyFormatting("&fBuy " + shop.amount + " for &e" + shop.buyPrice), false);
    }

    // Helper to count items in chest
    private static int countStock(Inventory inv, String itemID) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            System.out.println(stack.getItem().toString());
            System.out.println(itemID);
            System.out.println(stack.getItem().toString().equals(itemID));

            if (stack.getItem().toString().equals(itemID)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
