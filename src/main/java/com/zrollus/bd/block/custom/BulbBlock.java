package com.zrollus.bd.block.custom;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BulbBlock extends net.minecraft.block.BulbBlock {

    public BulbBlock(Settings settings) {
        super(settings);
    }

    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            world.setBlockState(pos, state.cycle(LIT));
        }
        // Returning SUCCESS matches the enum constant from your decompiled code!
        return ActionResult.SUCCESS;
    }
}

