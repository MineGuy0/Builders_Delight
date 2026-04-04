package com.zrollus.bd.GUI;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;

public class InvSeeScreen extends HandledScreen<InvSeeScreenHandler> {
    private final GameProfile adminProfile;
    private GameProfile targetProfile;
    private static final Identifier TEXTURE = new Identifier("bd", "textures/gui/container/invsee.png");

    public InvSeeScreen(InvSeeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        this.backgroundWidth = 368;
        this.backgroundHeight = 166;
        this.titleY = -1000;          // Hide "InvSee | Name"
        this.playerInventoryTitleY = -1000; // Hide "Inventory"

        // Admin is easy: it's just the person sitting at the computer
        this.adminProfile = inventory.player.getGameProfile();

        // Target: Extract from the title "InvSee | PlayerName"
        String titleContent = title.getString();
        if (titleContent.contains("|")) {
            String name = titleContent.split("\\|")[1].trim();

            // 1. Create a basic profile
            GameProfile basicProfile = new GameProfile(null, name);

            // 2. Tell the client to fetch the UUID and Skin data for this name
            // This is asynchronous, so it might show Steve/Alex for a second, then "pop" into the real skin
            MinecraftClient.getInstance().getSessionService().fillProfileProperties(basicProfile, true);
            this.targetProfile = basicProfile;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // 2. THIS IS THE MISSING PIECE: Draw the actual GUI box
        // Parameters: texture, x, y, u, v, width, height, textureWidth, textureHeight
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 512, 256);

        // Draw Admin Head (Green Side)
        if (adminProfile != null) {
            drawFace(context, adminProfile, x + 134, y + 7);
        }

        // Draw Target Head (Brown Side)
        if (targetProfile != null) {
            drawFace(context, targetProfile, x + 200, y + 7);
        }
    }

    private void drawFace(DrawContext context, GameProfile profile, int x, int y) {
        if (profile == null) return;

        // This is the most reliable way to get the skin Identifier on modern Fabric
        Identifier skin = client.getSkinProvider().loadSkin(profile);

        // Draw the face
        context.drawTexture(skin, x, y, 16, 16, 8, 8, 8, 8, 64, 64);
        // Draw the hat layer
        context.drawTexture(skin, x, y, 16, 16, 40, 8, 8, 8, 64, 64);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // This draws text on TOP of the background
        String targetName = targetProfile != null ? targetProfile.getName() : "Unknown";

        // Draw Admin Name
        context.drawText(this.textRenderer, adminProfile.getName(), 8, 13, 0xFFFFFF, false);

        // Draw Target Name
        context.drawText(this.textRenderer, targetName, 220, 13, 0xFFFFFF, false);
    }
}