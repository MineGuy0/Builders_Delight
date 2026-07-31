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
import com.zrollus.bd.world.TheBelowWorld;

public final class DeadTreeFeature extends Feature<DefaultFeatureConfig> {
    public DeadTreeFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();
        BlockPos floor = findFloor(world, origin.getX(), origin.getZ());
        if (floor == null) return false;

        int height = 5 + random.nextInt(5);
        BlockState trunk = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
        for (int y = 1; y <= height; y++) {
            BlockPos pos = floor.up(y);
            if (!world.getBlockState(pos).isAir()) return y > 3;
            world.setBlockState(pos, trunk, 2);
        }

        BlockPos crown = floor.up(height - 1);
        int branches = 3 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            Direction direction = Direction.Type.HORIZONTAL.random(random);
            int length = 2 + random.nextInt(3);
            BlockPos branch = crown.up(random.nextInt(2));
            for (int step = 1; step <= length; step++) {
                branch = branch.offset(direction);
                if (step == length && random.nextBoolean()) branch = branch.up();
                if (world.getBlockState(branch).isAir()) {
                    world.setBlockState(branch, trunk.with(net.minecraft.block.PillarBlock.AXIS,
                            direction.getAxis()), 2);
                }
            }
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (random.nextBoolean()) {
                BlockPos root = floor.offset(direction);
                if (world.getBlockState(root).isAir()) {
                    world.setBlockState(root, Blocks.BLACKSTONE.getDefaultState(), 2);
                }
            }
        }
        return true;
    }

    static BlockPos findFloor(StructureWorldAccess world, int x, int z) {
        for (int y = TheBelowWorld.SURFACE_Y + 17; y >= TheBelowWorld.SURFACE_Y - 2; y--) {
            BlockPos floor = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(floor);
            if ((state.isOf(Blocks.SOUL_SAND) || state.isOf(Blocks.SOUL_SOIL))
                    && world.getBlockState(floor.up()).isAir()
                    && world.getBlockState(floor.up(2)).isAir()) {
                return floor;
            }
        }
        return null;
    }
}
