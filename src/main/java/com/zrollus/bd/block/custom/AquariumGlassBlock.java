package com.zrollus.bd.block.custom;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;


public class AquariumGlassBlock extends Block implements Waterloggable {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // Thin shapes inset by 1px (to prevent z-fighting)
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 1);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(15, 0, 0, 16, 16, 16);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 1, 16);
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(0, 15, 0, 16, 16, 16);

    public AquariumGlassBlock(FabricBlockSettings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.UP)
                .with(WATERLOGGED, false));
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState neighborState, Direction direction) {
        // Cull inner faces when next to the same block
        if (neighborState.getBlock() instanceof AquariumGlassBlock) {
            return true;
        }
        return super.isSideInvisible(state, neighborState, direction);
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluid = ctx.getWorld().getFluidState(ctx.getBlockPos());
        PlayerEntity player = ctx.getPlayer();
        Direction side = ctx.getSide();

        // Sneak placement: place facing clicked side
        if (player != null && player.isSneaking()) {
            return this.getDefaultState()
                    .with(FACING, side)
                    .with(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
        }

        // Connect to existing aquarium glass
        BlockState clicked = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(side));
        if (clicked.getBlock() instanceof AquariumGlassBlock && clicked.get(FACING) != side.getOpposite()) {
            side = clicked.get(FACING);
        }

        return this.getDefaultState()
                .with(FACING, side)
                .with(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
        };
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
}

