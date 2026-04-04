package com.zrollus.bd.EventHandler;

import com.zrollus.bd.Lib.LocationStorageLib;
import com.zrollus.bd.Lib.MessageLib;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.time.LocalDateTime;

public class PlayerLifecycleHandler {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

            if (data == null) {
                data = new PlayerDataModel();
                data.lastKnownName = player.getEntityName();
                LocationStorageLib.savePlayerData(server, player.getUuid(), data);
                return;
            }

            if (!data.mail.isEmpty()) player.sendMessage(MessageLib.mailJoinNotification(data.mail.size()), false);

            if (data.worldId != null && data.coords != null) {
                var worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(data.worldId));
                var targetWorld = server.getWorld(worldKey);
                if (targetWorld != null) player.teleport(targetWorld, data.coords.getX(), data.coords.getY(), data.coords.getZ(), data.lastYaw, data.lastPitch);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());
            data.coords = player.getPos();
            data.lastYaw = player.getYaw();
            data.lastPitch = player.getPitch();
            data.worldId = player.getWorld().getRegistryKey().getValue().toString();
            data.lastLogonTime = LocalDateTime.now().toString();
            LocationStorageLib.savePlayerData(server, player.getUuid(), data);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                LocationStorageLib.saveBackLocation(player);
                player.sendMessage(MessageLib.deathLocation((int)player.getX(), (int)player.getY(), (int)player.getZ()));
            }
        });
    }
}