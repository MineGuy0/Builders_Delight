package com.zrollus.bd.block.custom;

import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class SoulBrazierBlock extends BrazierBlock {
    public SoulBrazierBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT)) {
            double x = (double)pos.getX() + 0.5;
            double y = (double)pos.getY() + 0.7; // Positioned in the bowl
            double z = (double)pos.getZ() + 0.5;

            if (random.nextDouble() < 0.1) {
                world.playSound(x, y, z, net.minecraft.sound.SoundEvents.BLOCK_CAMPFIRE_CRACKLE, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f, false);
            }

            // Custom Soul Particles
            for (int i = 0; i < 2; i++) {
                world.addParticle(ParticleTypes.SOUL,
                        x + (random.nextDouble() - 0.5) * 0.5,
                        y + random.nextDouble() * 0.5,
                        z + (random.nextDouble() - 0.5) * 0.5,
                        0.0, 0.07, 0.0);
            }

            // Add blue soul flame embers
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}