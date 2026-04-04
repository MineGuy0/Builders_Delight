package com.zrollus.bd.Lib;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    // Matches &#ffffff or &#fff
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})");

    public static Text format(String text) {
        if (text == null || text.isEmpty()) return Text.empty();

        MutableText fullText = Text.empty();
        Matcher matcher = HEX_PATTERN.matcher(text);
        int lastPos = 0;

        while (matcher.find()) {
            // 1. Add everything BEFORE the hex code (and process & codes there)
            String before = text.substring(lastPos, matcher.start());
            fullText.append(Text.literal(before.replace('&', '§')));

            // 2. Parse the Hex
            String hex = matcher.group(1);
            if (hex.length() == 3) {
                hex = ""+hex.charAt(0)+hex.charAt(0)+hex.charAt(1)+hex.charAt(1)+hex.charAt(2)+hex.charAt(2);
            }
            final String finalHex = "#" + hex;

            // 3. Find where this color ends (either the next &# or end of string)
            int nextMatch = text.length();
            Matcher nextMatcher = HEX_PATTERN.matcher(text);
            if (nextMatcher.find(matcher.end())) {
                nextMatch = nextMatcher.start();
            }

            String coloredPart = text.substring(matcher.end(), nextMatch);

            // 4. Apply the TRUE RGB Color
            fullText.append(Text.literal(coloredPart.replace('&', '§')).setStyle(
                    Text.empty().getStyle().withColor(TextColor.parse(finalHex))
            ));

            lastPos = nextMatch;
        }

        // 5. Add any remaining text
        if (lastPos < text.length()) {
            fullText.append(Text.literal(text.substring(lastPos).replace('&', '§')));
        }

        return fullText;
    }
}