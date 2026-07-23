package com.zrollus.bd.GUI;

import com.zrollus.bd.Entity.PlayerShopkeeperEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class ShopkeeperEditorScreen extends HandledScreen<ShopkeeperEditorScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("bd", "textures/gui/container/shopkeeper_editor.png");
    private ButtonWidget primaryPrevious;
    private ButtonWidget primaryNext;
    private ButtonWidget secondaryPrevious;
    private ButtonWidget secondaryNext;
    private ButtonWidget ageOrLevel;
    private ButtonWidget tertiary;
    private ButtonWidget playerSkin;
    private ButtonWidget previousPage;
    private ButtonWidget nextPage;
    private ButtonWidget openClose;
    private ButtonWidget silence;
    private ButtonWidget move;

    public ShopkeeperEditorScreen(ShopkeeperEditorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 254;
        this.backgroundHeight = 169;
        this.titleY = -10_000;
        this.playerInventoryTitleY = -10_000;
    }

    @Override
    protected void init() {
        super.init();
        int left = x + 184;
        int right = x + 218;
        primaryPrevious = button("< P", left, y + 82, 31, () -> clickAction(29));
        primaryNext = button("P >", right, y + 82, 31, () -> clickAction(28));
        secondaryPrevious = button("< B", left, y + 100, 31, () -> clickAction(32));
        secondaryNext = button("B >", right, y + 100, 31, () -> clickAction(30));
        ageOrLevel = button("T", left, y + 118, 31, () -> clickAction(33));
        tertiary = button("Age", right, y + 118, 31, () -> clickAction(34));
        playerSkin = button("Change Skin", left, y + 100, 65, () -> clickAction(37));
        openClose = button("Close", left, y + 132, 31, 14, () -> clickAction(38));
        silence = button("Mute", right, y + 132, 31, 14, () -> clickAction(45));
        previousPage = button("<", left, y + 148, 16, () -> clickAction(27));
        move = button("Move", x + 201, y + 148, 32, () -> clickAction(42));
        nextPage = button(">", x + 234, y + 148, 15, () -> clickAction(35));
    }

    private ButtonWidget button(String label, int buttonX, int buttonY, int width, Runnable action) {
        return button(label, buttonX, buttonY, width, 16, action);
    }

    private ButtonWidget button(String label, int buttonX, int buttonY, int width, int height, Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(label), ignored -> action.run())
                .dimensions(buttonX, buttonY, width, height).build());
    }

    private void clickAction(int slot) {
        if (client == null || client.player == null || client.interactionManager == null) return;
        client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 254, 169);

        Entity preview = client == null || client.world == null ? null : client.world.getEntityById(handler.getPreviewEntityId());
        boolean playerObject = preview instanceof PlayerShopkeeperEntity;
        primaryPrevious.visible = primaryNext.visible = secondaryPrevious.visible = secondaryNext.visible = !playerObject;
        ageOrLevel.visible = tertiary.visible = !playerObject;
        playerSkin.visible = playerObject;
        openClose.setMessage(Text.literal(handler.isShopClosed() ? "Open" : "Close"));
        silence.setMessage(Text.literal(handler.isShopSilent() ? "Sound" : "Mute"));
        previousPage.active = handler.getPageNumber() > 1;
        nextPage.active = handler.getPageNumber() < handler.getPageCount();

        if (preview instanceof LivingEntity living) {
            InventoryScreen.drawEntity(context, x + 183, y + 13, x + 251, y + 80,
                    30, 0.0625F, 0.0F, 0.0F, living);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Not loaded"), x + 217, y + 42, 0xAAAAAA);
        }
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(handler.getPageNumber() + "/" + handler.getPageCount()), x + 217, y + 139, 0xFFFFFF);
    }
}
