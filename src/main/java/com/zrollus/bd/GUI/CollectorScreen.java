package com.zrollus.bd.GUI;

import com.zrollus.bd.Entity.CollectorBlockEntity;
import com.zrollus.bd.block.custom.CollectorBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Dedicated shopkeeper-style editor shared by the ranged item and XP collectors. */
@Environment(EnvType.CLIENT)
public final class CollectorScreen extends HandledScreen<CollectorScreenHandler> {
    private static final Identifier ITEM_BACKGROUND =
            Identifier.of("bd", "textures/gui/container/item_collector.png");
    private static final Identifier EXPERIENCE_BACKGROUND =
            Identifier.of("bd", "textures/gui/container/experience_collector.png");
    private static final Identifier ITEM_ICON =
            Identifier.of("bd", "textures/item/item_collector.png");
    private static final Identifier EXPERIENCE_ICON =
            Identifier.of("bd", "textures/item/experience_collector.png");

    public CollectorScreen(CollectorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 236;
        backgroundHeight = 222;
        titleY = -10_000;
        playerInventoryTitleY = -10_000;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        boolean experience = handler.getCollectorType() == CollectorBlock.Type.EXPERIENCE;
        Identifier background = experience ? EXPERIENCE_BACKGROUND : ITEM_BACKGROUND;
        context.drawTexture(background, x, y, 0, 0, backgroundWidth, backgroundHeight,
                backgroundWidth, backgroundHeight);

        context.drawTextWithShadow(textRenderer,
                experience ? Text.literal("Ranged XP Collector") : Text.literal("Ranged Item Collector"),
                x + 6, y + 5, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Inventory"), x + 8, y + 132, 0x555A5C);

        int panelCenter = x + 204;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Region"), panelCenter, y + 6, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(shortShape(handler.getShape().name())), panelCenter, y + 23, 0xF0F0F0);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("R " + handler.getRangeX() + "/" + handler.getRangeY() + "/" + handler.getRangeZ()),
                panelCenter, y + 34, 0xA9F0C0);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("O " + signed(handler.getOffsetX()) + "/" + signed(handler.getOffsetY())
                        + "/" + signed(handler.getOffsetZ())),
                panelCenter, y + 45, 0xA7D7FF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("32 max"), panelCenter, y + 56, 0xAAAAAA);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Power"), panelCenter, y + 73, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(shortMode(handler.getRedstoneMode())), panelCenter, y + 85, 0xFFCB66);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(handler.getInterval() + "t scan"), panelCenter, y + 96, 0xD0D0D0);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(handler.previewEnabled() ? "Pinned on" : "Pinned off"),
                panelCenter, y + 107, handler.previewEnabled() ? 0x7CFF99 : 0xFF7777);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(experience ? "Experience" : "Items"), panelCenter, y + 132, 0xFFFFFF);
        if (experience) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(handler.getStoredExperience() + " XP"), panelCenter, y + 144, 0xC8F040);
            context.drawTexture(EXPERIENCE_ICON, x + 188, y + 154, 0, 0, 32, 32, 32, 32);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(handler.isBlacklist() ? "Blacklist" : "Whitelist"),
                    panelCenter, y + 144, handler.isBlacklist() ? 0xFF7777 : 0x7CFF99);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(handler.getStackLimit() + "/scan"), panelCenter, y + 155, 0xD0D0D0);
            context.drawTexture(ITEM_ICON, x + 188, y + 164, 0, 0, 32, 32, 32, 32);
        }

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("L +  R -"), panelCenter, y + 201, 0xE0E0E0);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Shift x8"), panelCenter, y + 211, 0xB8B8B8);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static String shortShape(String value) {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String shortMode(CollectorBlockEntity.RedstoneMode mode) {
        return switch (mode) {
            case HIGH -> "High signal";
            case LOW -> "Low signal";
            case PULSE -> "Pulse";
            case ALWAYS -> "Always";
        };
    }
}
