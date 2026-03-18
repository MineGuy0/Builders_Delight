package com.zrollus.bd.Lib;

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

    // --- GENERIC ERRORS ---
    public static final Text PLAYER_ONLY = Text.literal("Only players can execute this command.").formatted(COLOR_ERROR);
    public static final Text PLAYER_NOT_FOUND = Text.literal("Player not found.").formatted(COLOR_ERROR);
    public static final Text NO_PERMISSION = Text.literal("You do not have permission to use this command.").formatted(COLOR_ERROR);

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

    public static Text tpaRequestAccepted(String targetName) {
        return Text.literal("Accepted teleport request, Teleporting to ").formatted(COLOR_TEXT)
                .append(Text.literal(targetName).formatted(COLOR_MAIN));
    }

    public static Text tpaRequestDenied(String targetName) {
        return Text.literal("Denied the teleport request from ").formatted(COLOR_TEXT)
                .append(Text.literal(targetName).formatted(COLOR_MAIN));
    }

    public static final Text TPA_TIMEOUT = Text.literal("Teleport request timed out.").formatted(Formatting.DARK_RED);
    public static final Text TPA_DENIED = Text.literal("Teleport request denied.").formatted(COLOR_ERROR);
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
}