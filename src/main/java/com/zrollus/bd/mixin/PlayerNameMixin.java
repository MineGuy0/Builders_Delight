package com.zrollus.bd.mixin;

import com.zrollus.bd.Lib.ColorUtils;
import com.zrollus.bd.Lib.LocationStorageLib;
import com.zrollus.bd.Lib.ModConfigHelper;
import com.zrollus.bd.Lib.libHelpers.PlayerDataModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerNameMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void bds$substituteNickname(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        MinecraftServer server = player.getServer();

        if (server == null) return;

        PlayerDataModel data = LocationStorageLib.getPlayerData(server, player.getUuid());

        if (data != null && data.nickname != null && !data.nickname.isEmpty()) {
            // 1. Get the prefix from your config handler
            String prefix = ModConfigHelper.get().nicknamePrefix;

            // 2. Safety check: If the config is missing the key, default to empty string
            if (prefix == null) {
                prefix = "";
            }

            // 3. Construct the text.
            // If prefix is "", it adds nothing to the start.
            Text formattedNick = Text.literal(prefix).append(ColorUtils.format(data.nickname));

            cir.setReturnValue(formattedNick);
        }
    }
}