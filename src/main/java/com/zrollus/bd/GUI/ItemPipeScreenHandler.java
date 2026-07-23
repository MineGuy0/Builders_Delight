package com.zrollus.bd.GUI;

import com.zrollus.bd.Entity.ItemPipeBlockEntity;
import com.zrollus.bd.block.custom.ItemPipeBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Three-row endpoint configuration with nine non-consuming whitelist slots. */
public final class ItemPipeScreenHandler extends GenericContainerScreenHandler {
    private static final int MENU_SIZE = 27;
    private final ItemPipeBlockEntity pipe;

    public ItemPipeScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.ITEM_PIPE, syncId, playerInventory, new SimpleInventory(MENU_SIZE), 3);
        this.pipe = null;
    }

    public ItemPipeScreenHandler(int syncId, PlayerInventory playerInventory, ItemPipeBlockEntity pipe) {
        super(ModScreenHandlers.ITEM_PIPE, syncId, playerInventory, new SimpleInventory(MENU_SIZE), 3);
        this.pipe = pipe;
        refresh();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return pipe == null || Inventory.canPlayerUse(pipe, player);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < MENU_SIZE) {
            if (pipe != null && player instanceof ServerPlayerEntity
                    && slotIndex >= 9 && slotIndex < 18) {
                pipe.setFilter(slotIndex - 9, getCursorStack());
                refresh();
            }
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    private void refresh() {
        if (pipe == null) return;
        SimpleInventory menu = (SimpleInventory) getInventory();
        menu.clear();
        ItemPipeBlock.Role role = pipe.getRole();
        menu.setStack(4, named((role == ItemPipeBlock.Role.INPUT
                        ? Items.HOPPER : Items.DROPPER).getDefaultStack(),
                title(pipe.getPipeColor().getName()) + " " + title(role.name().toLowerCase())
                        + " pipe - adjacent inventories connect automatically"));
        for (int i = 0; i < 9; i++) {
            ItemStack filter = pipe.getFilter(i);
            menu.setStack(9 + i, filter.isEmpty()
                    ? named(Items.GRAY_STAINED_GLASS_PANE.getDefaultStack(),
                            "Whitelist slot " + (i + 1) + " (empty = allow all)")
                    : filter.copy());
        }
        boolean powered = pipe.getCachedState().contains(net.minecraft.state.property.Properties.POWERED)
                && pipe.getCachedState().get(net.minecraft.state.property.Properties.POWERED);
        menu.setStack(22, named((powered ? Items.REDSTONE_TORCH : Items.TORCH).getDefaultStack(),
                powered ? "Powered - next pulse transfers one stack" : "Unpowered - transfers are disabled"));
        menu.setStack(26, named(Items.BOOK.getDefaultStack(),
                "Empty whitelist = allow all. Colored pipes only join the same color."));
        sendContentUpdates();
    }

    private static String title(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.GOLD));
        return stack;
    }
}
