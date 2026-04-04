package com.zrollus.bd.GUI;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PlayerVaultScreen extends HandledScreen<PlayerVaultScreenHandler> {
    // Path to your custom texture with the shield/lock icon
    private static final Identifier TEXTURE = new Identifier("bd", "textures/gui/container/player_vault.png");

    public PlayerVaultScreen(PlayerVaultScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        // These MUST match the dimensions of your player_vault.png
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;

        // Push the title text 26 pixels right to avoid overlapping the vault icon
        this.titleX = 26;

        // Position the "Inventory" label above the player's items
        this.playerInventoryTitleY = 128;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draws the 176x222 texture from your resources
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Standard rendering calls
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}