package com.zrollus.bd.shopkeeper;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Vanilla's shift-trade path assumes every server-side Merchant is also an Entity
 * when it plays the success sound. ShopkeeperMerchant is deliberately data-backed,
 * so reproduce the vanilla transfer without that unsafe cast.
 */
final class ShopkeeperMerchantScreenHandler extends MerchantScreenHandler {
    ShopkeeperMerchantScreenHandler(int syncId, PlayerInventory playerInventory, ShopkeeperMerchant merchant) {
        super(syncId, playerInventory, merchant);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasStack()) return original;

        ItemStack moving = slot.getStack();
        original = moving.copy();
        if (index == 2) {
            if (!insertItem(moving, 3, 39, true)) return ItemStack.EMPTY;
            slot.onQuickTransfer(moving, original);
        } else if (index == 0 || index == 1) {
            if (!insertItem(moving, 3, 39, false)) return ItemStack.EMPTY;
        } else if (index >= 3 && index < 30) {
            if (!insertItem(moving, 30, 39, false)) return ItemStack.EMPTY;
        } else if (index >= 30 && index < 39) {
            if (!insertItem(moving, 3, 30, false)) return ItemStack.EMPTY;
        }

        if (moving.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;

        // This call depletes the payment stacks and invokes ShopkeeperMerchant.trade.
        slot.onTakeItem(player, moving);
        return original;
    }
}
