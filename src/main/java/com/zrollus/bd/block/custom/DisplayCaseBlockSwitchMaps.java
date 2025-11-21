package com.zrollus.bd.block.custom;

import net.minecraft.util.math.Direction;
import net.minecraft.block.enums.WallMountLocation;

// Synthetic class for switch statements
class DisplayCaseBlockSwitchMaps {
    static final int[] WALL_MOUNT_LOCATION_MAP = new int[WallMountLocation.values().length];
    static final int[] DIRECTION_MAP = new int[Direction.values().length];

    static {
        try {
            WALL_MOUNT_LOCATION_MAP[WallMountLocation.FLOOR.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {}

        try {
            WALL_MOUNT_LOCATION_MAP[WallMountLocation.CEILING.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {}

        try {
            WALL_MOUNT_LOCATION_MAP[WallMountLocation.WALL.ordinal()] = 3;
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