package com.zrollus.bd.GUI;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<PlayerVaultScreenHandler> PLAYER_VAULT =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("bd", "player_vault"),
                    new ScreenHandlerType<>(PlayerVaultScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<InvSeeScreenHandler> INVSEE =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("bd", "invsee"),
                    new ScreenHandlerType<>(InvSeeScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<ShopkeeperEditorScreenHandler> SHOPKEEPER_EDITOR =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("bd", "shopkeeper_editor"),
                    new ScreenHandlerType<>(ShopkeeperEditorScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<ItemPipeScreenHandler> ITEM_PIPE =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("bd", "item_pipe"),
                    new ScreenHandlerType<>(ItemPipeScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** Forces all common screen handler registrations during mod initialization. */
    public static void register() {}

}
