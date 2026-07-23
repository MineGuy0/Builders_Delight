package com.zrollus.bd.essentials;

import com.zrollus.bd.Lib.ModConfigHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

public final class EssentialsSystem {
    private static final Map<UUID, Vec3d> LAST_POSITIONS = new HashMap<>();
    private static int ticks;

    private EssentialsSystem() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(EssentialsSystem::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            EssentialsManager.PlayerState state = EssentialsManager.get(server).player(player.getUuid());
            state.lastActive = System.currentTimeMillis();
            if (state.god) player.addCommandTag("bd_god");
            if (state.vanished) {
                player.setInvisible(true);
                syncVanishVisibility(player, true);
            }
            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                if (other != player && EssentialsManager.get(server).player(other.getUuid()).vanished) {
                    player.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(other.getUuid())));
                }
            }
            enforceJail(server, player);
        });
    }

    /** Synchronizes the tab list as well as the tracked invisibility used by the client renderer. */
    public static void syncVanishVisibility(ServerPlayerEntity player, boolean vanished) {
        for (ServerPlayerEntity viewer : player.getServer().getPlayerManager().getPlayerList()) {
            if (viewer == player) continue;
            if (vanished) {
                viewer.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(player.getUuid())));
            } else {
                viewer.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(List.of(player)));
            }
        }
    }

    public static void markActive(ServerPlayerEntity player) {
        EssentialsManager manager = EssentialsManager.get(player.getServer());
        EssentialsManager.PlayerState state = manager.player(player.getUuid());
        state.lastActive = System.currentTimeMillis();
        if (state.afk) setAfk(player, false, true);
    }

    public static void setAfk(ServerPlayerEntity player, boolean afk, boolean broadcast) {
        EssentialsManager manager = EssentialsManager.get(player.getServer());
        EssentialsManager.PlayerState state = manager.player(player.getUuid());
        if (state.afk == afk) return;
        state.afk = afk;
        state.lastActive = System.currentTimeMillis();
        manager.changed();
        Text message = Text.literal(player.getName().getString() + (afk ? " is now AFK." : " is no longer AFK."))
                .formatted(afk ? Formatting.YELLOW : Formatting.GREEN);
        if (broadcast) player.getServer().getPlayerManager().broadcast(message, false);
        else player.sendMessage(message, false);
    }

    private static void tick(MinecraftServer server) {
        if (++ticks % 20 != 0) return;
        long now = System.currentTimeMillis();
        EssentialsManager manager = EssentialsManager.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            EssentialsManager.PlayerState state = manager.player(player.getUuid());
            Vec3d previous = LAST_POSITIONS.put(player.getUuid(), player.getPos());
            if (previous == null || previous.squaredDistanceTo(player.getPos()) > 0.0025) {
                if (state.afk && ModConfigHelper.get().cancelAfkOnMove) setAfk(player, false, true);
                state.lastActive = now;
            }
            if (!state.afk && ModConfigHelper.get().autoAfkSeconds > 0
                    && now - state.lastActive >= ModConfigHelper.get().autoAfkSeconds * 1000L) {
                setAfk(player, true, true);
            }
            if (state.god && !player.getCommandTags().contains("bd_god")) player.addCommandTag("bd_god");
            if (state.vanished && !player.isInvisible()) player.setInvisible(true);
            enforceJail(server, player);
        }
    }

    private static void enforceJail(MinecraftServer server, ServerPlayerEntity player) {
        EssentialsManager manager = EssentialsManager.get(server);
        EssentialsManager.JailRecord record = manager.jailed.get(player.getUuid());
        if (record == null) return;
        if (record.until > 0 && record.until <= System.currentTimeMillis()) {
            manager.jailed.remove(player.getUuid());
            manager.changed();
            player.sendMessage(Text.literal("Your jail sentence has ended.").formatted(Formatting.GREEN));
            return;
        }
        EssentialsManager.SavedLocation location = manager.jails.get(record.jail);
        if (location == null) return;
        Identifier id = Identifier.tryParse(location.world());
        if (id == null) return;
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
        if (world == null) return;
        if (player.getWorld() != world || player.getPos().squaredDistanceTo(Vec3d.ofCenter(location.pos())) > 100.0) {
            player.teleport(world, location.pos().getX() + .5, location.pos().getY(), location.pos().getZ() + .5,
                    location.yaw(), location.pitch());
        }
    }
}
