package com.zrollus.bd.Entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SingleStackInventory; // Standard for 1.20.1
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
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
        if (slot != 0 || amount <= 0 || this.stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = this.stack.split(amount);
        if (this.stack.isEmpty()) this.stack = ItemStack.EMPTY;
        markChangedAndSync();
        return removed;
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
        this.stack = stack.isEmpty() ? ItemStack.EMPTY : stack;
        markChangedAndSync();
    }

    private void markChangedAndSync() {
        this.markDirty();
        if (this.world != null) {
            // Tells the renderer to redraw because the item changed
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        // Empty stacks are omitted when written. Always clear the previous
        // client-side value first so an empty update cannot leave a stale item
        // in the block entity renderer.
        this.stack = ItemStack.EMPTY;
        if (nbt.contains("Item", 10)) {
            this.stack = ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Item"));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        // Original logic: don't save empty data
        if (!this.stack.isEmpty()) {
            nbt.put("Item", this.stack.encode(registries));
        }
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
