package com.zrollus.bd.Lib;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Locale;

/** Essentials-style formatting for the custom name of the held item. */
public final class ItemNameCommand {
    private static final int MAX_NAME_LENGTH = 256;
    private static final Style ITEM_NAME_BASE_STYLE = Style.EMPTY.withItalic(false);

    private ItemNameCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("itemname")
                        .requires(source -> PermissionCompat.check(source, "command.itemname", 0))
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ItemNameCommand::renameHeldItem))));
    }

    private static int renameHeldItem(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        String input = StringArgumentType.getString(context, "name");

        if (stack.isEmpty()) {
            context.getSource().sendError(Text.literal("Hold the item you want to rename in your main hand."));
            return 0;
        }
        if (input.length() > MAX_NAME_LENGTH) {
            context.getSource().sendError(Text.literal("Item names can be at most " + MAX_NAME_LENGTH + " characters long."));
            return 0;
        }

        MutableText name = parseLegacyFormatting(input);
        if (name.getString().isBlank()) {
            context.getSource().sendError(Text.literal("The item name cannot be empty."));
            return 0;
        }

        stack.set(DataComponentTypes.CUSTOM_NAME, name);
        context.getSource().sendFeedback(
                () -> Text.literal("Item renamed to ").append(name.copy()),
                false
        );
        return 1;
    }

    public static MutableText parseLegacyFormatting(String input) {
        MutableText result = Text.empty();
        StringBuilder segment = new StringBuilder();
        Style style = ITEM_NAME_BASE_STYLE;

        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if ((current == '&' || current == '\u00a7') && index + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(index + 1));
                if (code == current) {
                    segment.append(current);
                    index++;
                    continue;
                }

                Integer hexColor = readHexColor(input, index, current);
                if (hexColor != null) {
                    appendSegment(result, segment, style);
                    style = ITEM_NAME_BASE_STYLE.withColor(TextColor.fromRgb(hexColor));
                    index += input.charAt(index + 1) == '#' ? 7 : 13;
                    continue;
                }

                Formatting formatting = formattingFor(code);
                if (formatting != null) {
                    appendSegment(result, segment, style);
                    if (formatting == Formatting.RESET) {
                        style = ITEM_NAME_BASE_STYLE;
                    } else if (formatting.isColor()) {
                        // Legacy color codes reset decorations such as bold and italic.
                        style = ITEM_NAME_BASE_STYLE.withFormatting(formatting);
                    } else {
                        style = style.withFormatting(formatting);
                    }
                    index++;
                    continue;
                }
            }
            segment.append(current);
        }

        appendSegment(result, segment, style);
        return result;
    }

    private static Integer readHexColor(String input, int index, char marker) {
        // Essentials-style &#RRGGBB.
        if (index + 7 < input.length() && input.charAt(index + 1) == '#') {
            String hex = input.substring(index + 2, index + 8);
            if (hex.matches("[0-9A-Fa-f]{6}")) return Integer.parseInt(hex, 16);
        }

        // Vanilla's expanded legacy form: &x&R&R&G&G&B&B.
        if (index + 13 < input.length() && Character.toLowerCase(input.charAt(index + 1)) == 'x') {
            StringBuilder hex = new StringBuilder(6);
            for (int part = 0; part < 6; part++) {
                int markerIndex = index + 2 + part * 2;
                char digit = input.charAt(markerIndex + 1);
                if (input.charAt(markerIndex) != marker || Character.digit(digit, 16) < 0) return null;
                hex.append(digit);
            }
            return Integer.parseInt(hex.toString(), 16);
        }
        return null;
    }

    private static void appendSegment(MutableText result, StringBuilder segment, Style style) {
        if (segment.isEmpty()) {
            return;
        }
        result.append(Text.literal(segment.toString()).setStyle(style));
        segment.setLength(0);
    }

    private static Formatting formattingFor(char code) {
        return switch (Character.toString(code).toLowerCase(Locale.ROOT)) {
            case "0" -> Formatting.BLACK;
            case "1" -> Formatting.DARK_BLUE;
            case "2" -> Formatting.DARK_GREEN;
            case "3" -> Formatting.DARK_AQUA;
            case "4" -> Formatting.DARK_RED;
            case "5" -> Formatting.DARK_PURPLE;
            case "6" -> Formatting.GOLD;
            case "7" -> Formatting.GRAY;
            case "8" -> Formatting.DARK_GRAY;
            case "9" -> Formatting.BLUE;
            case "a" -> Formatting.GREEN;
            case "b" -> Formatting.AQUA;
            case "c" -> Formatting.RED;
            case "d" -> Formatting.LIGHT_PURPLE;
            case "e" -> Formatting.YELLOW;
            case "f" -> Formatting.WHITE;
            case "k" -> Formatting.OBFUSCATED;
            case "l" -> Formatting.BOLD;
            case "m" -> Formatting.STRIKETHROUGH;
            case "n" -> Formatting.UNDERLINE;
            case "o" -> Formatting.ITALIC;
            case "r" -> Formatting.RESET;
            default -> null;
        };
    }
}
