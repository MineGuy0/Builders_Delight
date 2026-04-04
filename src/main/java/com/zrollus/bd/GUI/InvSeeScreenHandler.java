package com.zrollus.bd.GUI;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class InvSeeScreenHandler extends ScreenHandler {
    private final Inventory targetInv;

    public InvSeeScreenHandler(int syncId, PlayerInventory adminInv) {
        this(syncId, adminInv, new SimpleInventory(41));
    }

    // 2. SERVER CONSTRUCTOR (Used by the Command)
    public InvSeeScreenHandler(int syncId, PlayerInventory adminInv, Inventory targetInv) {
        super(ModScreenHandlers.INVSEE, syncId);
        this.targetInv = targetInv;

        int yOffset = 18;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(adminInv, col + row * 9 + 9, 8 + col * 18, 66+yOffset));

            }
            yOffset+=18;
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(adminInv, col, 8 + col * 18, 142));
        }

        int xOffset = 175;
        yOffset = 18;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(targetInv, col + row * 9 + 9, xOffset + 8 + col * 18, 66+yOffset));

            }
            yOffset+=18;
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(targetInv, col, xOffset + 8 + col * 18, 142));
        }

        int armorY = 62;
        int armorGap = 0;
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(adminInv, 39 - i, 26 + i * 18 + armorGap, armorY));
            this.addSlot(new Slot(targetInv, 39 - i, xOffset + 26 + i * 18 + armorGap, armorY));
            armorGap+=18;
        }

        this.addSlot(new Slot(adminInv, 40, 134, armorY-35));
        this.addSlot(new Slot(targetInv, 40, xOffset + 134, armorY-35));

    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            itemStack = originalStack.copy();

            // index < 36 is Admin's main/hotbar
            if (index < 36) {
                if (!this.insertItem(originalStack, 36, 72, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // index 36-71 is Target's main/hotbar
            else if (index >= 36 && index < 72) {
                if (!this.insertItem(originalStack, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return itemStack;
    }
    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Check if the action is a "Pick" (Middle Click) and player is in Creative
        if (actionType == SlotActionType.PICKUP_ALL && player.getAbilities().creativeMode) {
            // Standard pick-block logic usually handles this, but if it feels "dead":
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}