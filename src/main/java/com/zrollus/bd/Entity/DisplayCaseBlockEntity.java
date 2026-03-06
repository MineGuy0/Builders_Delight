package com.zrollus.bd.Entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SingleStackInventory; // Standard for 1.20.1
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

public class DisplayCaseBlockEntity extends BlockEntity implements SingleStackInventory {
    // Original bytecode uses a default field assignment
    private ItemStack stack = ItemStack.EMPTY;

    public DisplayCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModEntities.DISPLAY_CASE, pos, state);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.stack;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return null;
    }

    // Modern mapping for your custom getStack()
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.setStack(stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
        this.markDirty();
        if (this.world != null) {
            // Tells the renderer to redraw because the item changed
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        // Original logic: check for tag to avoid errors
        if (nbt.contains("Item", 10)) {
            this.stack = ItemStack.fromNbt(nbt.getCompound("Item"));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        // Original logic: don't save empty data
        if (!this.stack.isEmpty()) {
            nbt.put("Item", this.stack.writeNbt(new NbtCompound()));
        }
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}