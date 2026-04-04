package com.zrollus.bd.GUI;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<PlayerVaultScreenHandler> PLAYER_VAULT =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier("bd", "player_vault"),
                    new ScreenHandlerType<>(PlayerVaultScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<InvSeeScreenHandler> INVSEE =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier("bd", "invsee"),
                    new ScreenHandlerType<>(InvSeeScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

}
