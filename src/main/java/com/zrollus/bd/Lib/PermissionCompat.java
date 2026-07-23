package com.zrollus.bd.Lib;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/** LuckPerms-compatible permission checks through Fabric Permissions API v0 (the 1.21.1-compatible API). */
public final class PermissionCompat {
    private PermissionCompat() {}

    public static boolean check(ServerCommandSource source, String node, int fallbackOpLevel) {
        return Permissions.check(source, normalize(node), fallbackOpLevel);
    }

    public static boolean check(ServerPlayerEntity player, String node, boolean fallback) {
        return Permissions.check(player, normalize(node), fallback);
    }

    private static String normalize(String node) {
        return node.startsWith("bd.") ? node : "bd." + node;
    }
}
