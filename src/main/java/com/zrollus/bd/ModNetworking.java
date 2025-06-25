package com.zrollus.bd;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class ModNetworking {
    public static final Identifier UPDATE_NUDGE_PACKET = new Identifier("buildersdelight", "update_nudge");
    public static final Identifier CONFIRM_NUDGE_PACKET = new Identifier("buildersdelight", "confirm_nudge");

    public static void init() {
        // Call this from your ModInitializer
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_NUDGE_PACKET, (server, player, handler, buf, responseSender) -> {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            com.zrollus.bd.utils.Nudge.setFor(player.getUuid(), new net.minecraft.util.math.Vec3i(x, y, z));
        });
    }
}