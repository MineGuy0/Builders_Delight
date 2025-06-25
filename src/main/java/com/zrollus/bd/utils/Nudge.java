package com.zrollus.bd.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Nudge {
    public static Vec3i offset = Vec3i.ZERO; // Client-side only

    private static final Map<UUID, Vec3i> serverOffsets = new ConcurrentHashMap<>();

    public static BlockPos applyTo(BlockPos pos, Direction face) {
        if (face.getAxis() == Direction.Axis.Y) {
            return pos.add(offset.getX(), 0, offset.getZ());
        } else {
            return pos.add(offset);
        }
    }

    // Server-specific offset per player
    public static void setFor(UUID playerId, Vec3i offset) {
        serverOffsets.put(playerId, offset);
    }

    public static Vec3i getFor(UUID playerId) {
        return serverOffsets.getOrDefault(playerId, Vec3i.ZERO);
    }

    public static void clear(UUID playerId) {
        serverOffsets.remove(playerId);
    }
}