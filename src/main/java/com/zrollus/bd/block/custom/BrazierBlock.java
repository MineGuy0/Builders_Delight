package com.zrollus.bd.block.custom;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class BrazierBlock extends IgnitableBlock {
    public static final BooleanProperty LEGS = BooleanProperty.of("legs");
    public static final BooleanProperty CHAIN = BooleanProperty.of("chain");

    public BrazierBlock(Settings settings) {
        // settings, litByDefault: false, handExtinguishable: true
        super(settings, false, true);
        this.setDefaultState(this.getDefaultState()
                .with(LEGS, false)
                .with(CHAIN, false));
    }

    private boolean isSteelBar(BlockState state) {
        // Replace with your custom Steel Bar block if you have one
        return state.isOf(Blocks.IRON_BARS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        return super.getPlacementState(ctx)
                .with(LEGS, isSteelBar(world.getBlockState(pos.down())))
                .with(CHAIN, isSteelBar(world.getBlockState(pos.up())));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state.with(LEGS, isSteelBar(neighborState));
        }
        if (direction == Direction.UP) {
            return state.with(CHAIN, isSteelBar(neighborState));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape base = Block.createCuboidShape(2, 0, 2, 14, 8, 14);
        if (state.get(LEGS)) {
            // Legs reach down into the block below
            VoxelShape legs = Block.createCuboidShape(4, -16, 4, 12, 0, 12);
            base = VoxelShapes.union(base, legs);
        }
        return base;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // Uses the new 1.20.1 damage source API
        if (state.get(LIT) && !entity.isFireImmune()) {
            entity.damage(world.getDamageSources().inFire(), 1.0f);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder); // Adds LIT and WATERLOGGED
        builder.add(LEGS, CHAIN);
    }
}