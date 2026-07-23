package com.zrollus.bd.block.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SeatEntity extends Entity {

    private BlockPos cushionPos; // Cushion/block position
    private boolean wasOccupied = false; // Tracks passenger mount state

    public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
        super(type, world);
    }

    // 1.21.1 FIX: Overhauled parameter profile utilizing DataTracker.Builder
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // No tracked data needed for now, leave empty but matching the signature
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        // No custom data to read yet
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        // No custom data to write yet
    }

    // Setter for cushion position
    public void setCushionPos(BlockPos pos) {
        this.cushionPos = pos;
    }

    // Getter for cushion position
    public BlockPos getCushionPos() {
        return this.cushionPos != null ? this.cushionPos : this.getBlockPos();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty();
    }

    @Override
    public net.minecraft.util.math.Vec3d getPassengerRidingPos(Entity passenger) {
        return this.getPos().add(0.0, 0.25, 0.0);
    }

    // 1.21.1 FIX: Legacy createSpawnPacket() has been deleted.
    // Base Minecraft automatically maps and creates spawn synchronization packets now!

    // Tick method to detect mount/unmount
    @Override
    public void tick() {
        super.tick();
        // If no passengers, remove seat and free cushion
        if (this.getPassengerList().isEmpty() && cushionPos != null) {
            if (!this.getWorld().isClient) {
                var state = this.getWorld().getBlockState(cushionPos);
                if (state.getBlock() instanceof com.zrollus.bd.block.custom.CushionBlock) {
                    this.getWorld().setBlockState(cushionPos, state.with(com.zrollus.bd.block.custom.CushionBlock.OCCUPIED, false), 3);
                }
            }
            this.discard();
        }
    }

    private void playSeatSound() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = getCushionPos();
            serverWorld.playSound(
                    null,
                    pos,
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), // <-- Add .value() here!
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }
}
