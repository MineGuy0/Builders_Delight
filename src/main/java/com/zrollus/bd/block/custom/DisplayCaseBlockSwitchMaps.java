package com.zrollus.bd.block.custom;

import net.minecraft.util.math.Direction;
import net.minecraft.block.enums.BlockFace; // Updated import

public class DisplayCaseBlockSwitchMaps { // Made public if accessed outside
    // Renamed map array to reflect BlockFace
    public static final int[] BLOCK_FACE_MAP = new int[BlockFace.values().length];
    public static final int[] DIRECTION_MAP = new int[Direction.values().length];

    static {
        try {
            BLOCK_FACE_MAP[BlockFace.FLOOR.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {}

        try {
            BLOCK_FACE_MAP[BlockFace.CEILING.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {}

        try {
            BLOCK_FACE_MAP[BlockFace.WALL.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {}

        try {
            DIRECTION_MAP[Direction.NORTH.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {}

        try {
            DIRECTION_MAP[Direction.EAST.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {}

        try {
            DIRECTION_MAP[Direction.SOUTH.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {}
    }
}