package com.zrollus.bd.mixin;

import com.zrollus.bd.Lib.ColorUtils;
import com.zrollus.bd.Lib.PermissionCompat;
import com.zrollus.bd.essentials.EssentialsManager;
import com.zrollus.bd.essentials.EssentialsSystem;
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
            , require = 0
    )
    private void bds$onBroadcast(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        // 1. Get the raw string from the signed message
        String rawContent = message.getContent().getString();

        EssentialsManager manager = EssentialsManager.get(sender.getServer());
        if (manager.isMuted(sender.getUuid())) {
            sender.sendMessage(net.minecraft.text.Text.literal("You are muted: " + manager.player(sender.getUuid()).muteReason)
                    .formatted(net.minecraft.util.Formatting.RED), false);
            ci.cancel();
            return;
        }
        EssentialsSystem.markActive(sender);

        // 2. Format it with our Hybrid Logic
        var formattedText = PermissionCompat.check(sender, "chat.color", false)
                ? ColorUtils.format(rawContent)
                : net.minecraft.text.Text.literal(rawContent);

        // 3. Re-broadcast the formatted version and cancel the original plain one
        // This avoids the "Signature" headache by sending it as a system message
        if (sender != null) {
            for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
                if (manager.player(recipient.getUuid()).ignored.contains(sender.getUuid())) continue;
                recipient.sendMessage(params.applyChatDecoration(formattedText), false);
            }
            ci.cancel(); // Stop the original uncolored message
        }
    }
}
