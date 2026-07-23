package com.zrollus.bd.Lib;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.registry.RegistryWrapper;
import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LockManager extends PersistentState {
    private final Map<BlockPos, UUID> locks = new HashMap<>();

    public void toggleLock(BlockPos pos, UUID owner, MinecraftServer server) {
        boolean wasLocked = locks.containsKey(pos);

        if (wasLocked) {
            locks.remove(pos);
        } else {
            locks.put(pos, owner);
        }

        // Handle Double-Height Blocks (Doors)
        var world = server.getOverworld();
        var state = world.getBlockState(pos);

        if (state.getBlock() instanceof net.minecraft.block.DoorBlock) {
            var half = state.get(net.minecraft.block.DoorBlock.HALF);
            BlockPos otherHalf = (half == net.minecraft.block.enums.DoubleBlockHalf.LOWER) ? pos.up() : pos.down();

            // Ensure the other half is actually part of the same door
            var otherState = world.getBlockState(otherHalf);
            if (otherState.getBlock() instanceof net.minecraft.block.DoorBlock) {
                if (wasLocked) {
                    locks.remove(otherHalf);
                } else {
                    locks.put(otherHalf, owner);
                }
            }
        }

        this.markDirty();
    }

    public UUID getOwner(BlockPos pos) { return locks.get(pos); }
    public boolean isLocked(BlockPos pos) { return locks.containsKey(pos); }

    // Boilerplate for saving to NBT
    // Boilerplate for saving to NBT
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        locks.forEach((pos, uuid) -> {
            NbtCompound entry = new NbtCompound();
            entry.putLong("pos", pos.asLong());
            entry.putUuid("owner", uuid); // Save the actual UUID
            list.add(entry);
        });
        nbt.put("Locks", list);
        return nbt;
    }

    public static LockManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        LockManager manager = new LockManager();
        NbtList list = nbt.getList("Locks", 10); // 10 is the ID for NbtCompound
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            BlockPos pos = BlockPos.fromLong(entry.getLong("pos"));
            UUID uuid = entry.getUuid("owner"); // Load the actual UUID
            manager.locks.put(pos, uuid);
        }
        return manager;
    }

    public static LockManager getServerState(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(new PersistentState.Type<>(LockManager::new, LockManager::fromNbt, null), "bd_locks");
    }
}
