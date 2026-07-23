package com.zrollus.bd.Lib;

import net.minecraft.text.Text;

/** Backwards-compatible entry point for Essentials-style legacy text formatting. */
public final class ColorUtils {
    private ColorUtils() {
    }

    public static Text format(String text) {
        return text == null || text.isEmpty()
                ? Text.empty()
                : ItemNameCommand.parseLegacyFormatting(text);
    }
}
