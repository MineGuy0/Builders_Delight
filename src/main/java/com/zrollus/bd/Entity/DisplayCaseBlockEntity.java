package com.zrollus.bd.Entity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

public class DisplayCaseBlockEntity extends BlockEntity {
    private ItemStack stack;

    public DisplayCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModEntities.DISPLAY_CASE, pos, state);
        this.setStack(ItemStack.EMPTY);
    }

    public void setStack(@Nullable ItemStack stack) {
        this.stack = (stack != null) ? stack : ItemStack.EMPTY;
        markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }
    }

    public ItemStack getStack() {
        return this.stack != null ? this.stack : ItemStack.EMPTY;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("Item", stack.writeNbt(new NbtCompound())); // OK
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        stack = ItemStack.fromNbt(nbt.getCompound("Item"));
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt(); // This is fine
    }

    @Nullable
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this); // This is correct
    }

}
