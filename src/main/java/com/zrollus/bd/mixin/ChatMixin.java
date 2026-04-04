package com.zrollus.bd.mixin;

import com.zrollus.bd.Lib.ColorUtils;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerManager.class)
public class ChatMixin {

    /**
     * This method is called right before a chat message is sent to everyone.
     * We use a "Redirect" or "Inject" here because the signature is very stable.
     */
    @Inject(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bds$onBroadcast(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        // 1. Get the raw string from the signed message
        String rawContent = message.getContent().getString();

        // 2. Format it with our Hybrid Logic
        var formattedText = ColorUtils.format(rawContent);

        // 3. Re-broadcast the formatted version and cancel the original plain one
        // This avoids the "Signature" headache by sending it as a system message
        if (sender != null) {
            sender.getServer().getPlayerManager().broadcast(
                    formattedText,
                    (player) -> params.applyChatDecoration(formattedText), // Keeps the [PlayerName] part
                    false
            );
            ci.cancel(); // Stop the original uncolored message
        }
    }
}