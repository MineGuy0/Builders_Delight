package com.zrollus.bd.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class DarkVillageFeature extends Feature<DefaultFeatureConfig> {
    private static final RegistryKey<LootTable> VILLAGE_LOOT = RegistryKey.of(
            RegistryKeys.LOOT_TABLE, Identifier.of("bd", "chests/dark_village"));
    public DarkVillageFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();
        BlockPos center = DeadTreeFeature.findFloor(world, origin.getX(), origin.getZ());
        if (center == null || !hasHeadroom(world, center, 7)) return false;

        buildSquare(world, center, 2, Blocks.SOUL_SOIL.getDefaultState());
        world.setBlockState(center.up(), Blocks.SOUL_CAMPFIRE.getDefaultState(), 2);

        int houses = 3 + random.nextInt(3);
        Direction direction = Direction.NORTH;
        for (int i = 0; i < houses; i++) {
            direction = direction.rotateYClockwise();
            int distance = 8 + (i % 2) * 3;
            BlockPos site = DeadTreeFeature.findFloor(world,
                    center.getX() + direction.getOffsetX() * distance,
                    center.getZ() + direction.getOffsetZ() * distance);
            if (site == null || !hasHeadroom(world, site, 6)) continue;
            layPath(world, center, site);
            buildHouse(world, site, direction.getOpposite(), random);
        }
        BlockPos chestPos = center.east(2).up();
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        if (world.getBlockEntity(chestPos) instanceof LootableContainerBlockEntity chest) {
            chest.setLootTable(VILLAGE_LOOT);
            chest.setLootTableSeed(random.nextLong());
        }
        return true;
    }

    private static void buildHouse(StructureWorldAccess world, BlockPos floor, Direction doorSide, Random random) {
        BlockState wall = random.nextBoolean()
                ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()
                : Blocks.DEEPSLATE_BRICKS.getDefaultState();
        buildSquare(world, floor, 3, Blocks.DARK_OAK_PLANKS.getDefaultState());

        for (int y = 1; y <= 4; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x) != 3 && Math.abs(z) != 3) continue;
                    BlockPos pos = floor.add(x, y, z);
                    boolean door = isDoor(x, z, doorSide) && y <= 2;
                    boolean window = y == 2 && ((Math.abs(x) == 3 && z == 0) || (Math.abs(z) == 3 && x == 0));
                    world.setBlockState(pos, door ? Blocks.AIR.getDefaultState()
                            : window ? Blocks.IRON_BARS.getDefaultState() : wall, 2);
                }
            }
        }

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) + Math.abs(z) > 7) continue;
                world.setBlockState(floor.add(x, 5, z), Blocks.BLACKSTONE.getDefaultState(), 2);
            }
        }
        world.setBlockState(floor.up(), Blocks.SOUL_LANTERN.getDefaultState(), 2);
    }

    private static boolean isDoor(int x, int z, Direction side) {
        return switch (side) {
            case NORTH -> z == -3 && x == 0;
            case SOUTH -> z == 3 && x == 0;
            case WEST -> x == -3 && z == 0;
            case EAST -> x == 3 && z == 0;
            default -> false;
        };
    }

    private static void layPath(StructureWorldAccess world, BlockPos from, BlockPos to) {
        int x = from.getX();
        int z = from.getZ();
        while (x != to.getX() || z != to.getZ()) {
            if (x != to.getX()) x += Integer.signum(to.getX() - x);
            else z += Integer.signum(to.getZ() - z);
            BlockPos floor = DeadTreeFeature.findFloor(world, x, z);
            if (floor != null) world.setBlockState(floor, Blocks.SOUL_SOIL.getDefaultState(), 2);
        }
    }

    private static void buildSquare(StructureWorldAccess world, BlockPos center, int radius, BlockState state) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                world.setBlockState(center.add(x, 0, z), state, 2);
            }
        }
    }

    private static boolean hasHeadroom(StructureWorldAccess world, BlockPos floor, int height) {
        for (int y = 1; y <= height; y++) {
            if (!world.getBlockState(floor.up(y)).isAir()) return false;
        }
        return true;
    }
}
