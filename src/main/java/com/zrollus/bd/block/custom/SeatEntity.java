package com.zrollus.bd.block.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
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

    // Required by Entity
    @Override
    protected void initDataTracker() {
        // No tracked data needed for now
    }

    @Override
    protected void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        // No custom data to read yet
    }

    @Override
    protected void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
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

    // Spawn packet for Fabric 1.20.1
    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    // Tick method to detect mount/unmount
    @Override
    public void tick() {
        super.tick();
        // If no passengers, remove seat and free cushion
        if (this.getPassengerList().isEmpty() && cushionPos != null) {
            if (!this.getWorld().isClient) {
                var state = this.getWorld().getBlockState(cushionPos);
                if (state.getBlock() instanceof com.zrollus.bd.block.custom.CushionBlock cushion) {
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
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

}
