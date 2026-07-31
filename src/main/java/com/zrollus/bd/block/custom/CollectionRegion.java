package com.zrollus.bd.block.custom;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Shared, persisted region configuration for ranged CraftBook-style machines. */
public final class CollectionRegion {
    public static final int MAX_RANGE = 32;
    public static final int MAX_OFFSET = 32;

    public enum Shape {
        BOX, SPHERE, CYLINDER;

        public Shape next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private int rangeX = 4;
    private int rangeY = 2;
    private int rangeZ = 4;
    private int offsetX;
    private int offsetY = 1;
    private int offsetZ;
    private Shape shape = Shape.BOX;

    public int rangeX() { return rangeX; }
    public int rangeY() { return rangeY; }
    public int rangeZ() { return rangeZ; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }
    public int offsetZ() { return offsetZ; }
    public Shape shape() { return shape; }

    public void setRangeX(int value) { rangeX = MathHelper.clamp(value, 1, MAX_RANGE); }
    public void setRangeY(int value) { rangeY = MathHelper.clamp(value, 1, MAX_RANGE); }
    public void setRangeZ(int value) { rangeZ = MathHelper.clamp(value, 1, MAX_RANGE); }
    public void setOffsetX(int value) { offsetX = MathHelper.clamp(value, -MAX_OFFSET, MAX_OFFSET); }
    public void setOffsetY(int value) { offsetY = MathHelper.clamp(value, -MAX_OFFSET, MAX_OFFSET); }
    public void setOffsetZ(int value) { offsetZ = MathHelper.clamp(value, -MAX_OFFSET, MAX_OFFSET); }
    public void setShape(int value) {
        shape = Shape.values()[Math.floorMod(value, Shape.values().length)];
    }

    public Vec3d center(BlockPos origin) {
        return Vec3d.ofCenter(origin).add(offsetX, offsetY, offsetZ);
    }

    public Box bounds(BlockPos origin) {
        Vec3d center = center(origin);
        return new Box(
                center.x - rangeX, center.y - rangeY, center.z - rangeZ,
                center.x + rangeX, center.y + rangeY, center.z + rangeZ
        );
    }

    public boolean contains(BlockPos origin, Vec3d point) {
        Vec3d center = center(origin);
        double dx = Math.abs(point.x - center.x) / rangeX;
        double dy = Math.abs(point.y - center.y) / rangeY;
        double dz = Math.abs(point.z - center.z) / rangeZ;
        return switch (shape) {
            case BOX -> dx <= 1.0 && dy <= 1.0 && dz <= 1.0;
            case SPHERE -> dx * dx + dy * dy + dz * dz <= 1.0;
            case CYLINDER -> dx * dx + dz * dz <= 1.0 && dy <= 1.0;
        };
    }

    /** Refuses to operate across unloaded chunks instead of loading them. */
    public boolean isLoaded(World world, BlockPos origin) {
        Box box = bounds(origin);
        int minChunkX = MathHelper.floor(box.minX) >> 4;
        int maxChunkX = MathHelper.floor(box.maxX) >> 4;
        int minChunkZ = MathHelper.floor(box.minZ) >> 4;
        int maxChunkZ = MathHelper.floor(box.maxZ) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (!world.isChunkLoaded(x, z)) return false;
            }
        }
        return box.minY >= world.getBottomY() && box.maxY <= world.getTopY();
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("range_x", rangeX);
        nbt.putInt("range_y", rangeY);
        nbt.putInt("range_z", rangeZ);
        nbt.putInt("offset_x", offsetX);
        nbt.putInt("offset_y", offsetY);
        nbt.putInt("offset_z", offsetZ);
        nbt.putInt("shape", shape.ordinal());
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("range_x")) setRangeX(nbt.getInt("range_x"));
        if (nbt.contains("range_y")) setRangeY(nbt.getInt("range_y"));
        if (nbt.contains("range_z")) setRangeZ(nbt.getInt("range_z"));
        setOffsetX(nbt.getInt("offset_x"));
        setOffsetY(nbt.getInt("offset_y"));
        setOffsetZ(nbt.getInt("offset_z"));
        setShape(nbt.getInt("shape"));
    }
}
