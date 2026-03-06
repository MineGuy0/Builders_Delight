package com.zrollus.bd.block.custom;

import net.minecraft.block.*;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;

public class BranchBlock extends Block {
    // Axis rotation
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    // Connection properties
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private static final BooleanProperty[] DIRECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    // Voxel shapes for each axis
    private static final VoxelShape SHAPE_Y = Block.createCuboidShape(4, 0, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_X = Block.createCuboidShape(0, 4, 4, 16, 12, 12);
    private static final VoxelShape SHAPE_Z = Block.createCuboidShape(4, 4, 0, 12, 12, 16);

    public BranchBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(AXIS, Direction.Axis.Y)
                .with(WATERLOGGED, false)
                .with(UP, false).with(DOWN, false)
                .with(NORTH, false).with(SOUTH, false)
                .with(EAST, false).with(WEST, false)
        );
    }


    // Rotate block
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        switch(rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> {
                if (state.get(AXIS) == Direction.Axis.X) return state.with(AXIS, Direction.Axis.Z);
                if (state.get(AXIS) == Direction.Axis.Z) return state.with(AXIS, Direction.Axis.X);
            }
        }
        return state;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WATERLOGGED, UP, DOWN, NORTH, SOUTH, EAST, WEST);
    }

    // Placement state
    // 1. Modified update logic: Only ADDS connections, never removes them
    private static BlockState addConnections(BlockState state, BlockView world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // If the neighbor is a branch, set that specific direction to true
            // If it's NOT a branch, we leave the current state's value alone!
            if (neighborState.getBlock() instanceof BranchBlock) {
                state = state.with(DIRECTIONS[dir.getId()], true);
            }
        }
        return state;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Start with default (all false) and only ADD what's currently around it
        BlockState state = this.getDefaultState().with(AXIS, ctx.getSide().getAxis());
        return addConnections(state, ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.offset(dir);
                BlockState neighborState = world.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof BranchBlock) {
                    // We update the neighbor to face US,
                    // but we use the opposite direction (dir.getOpposite())
                    world.setBlockState(neighborPos, neighborState.with(DIRECTIONS[dir.getOpposite().getId()], true), 3);
                }
            }
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Keep waterlogged logic
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        // Return state as-is. Do NOT call addConnections here.
        return state;
    }

    // Outline and collision shapes
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = switch (state.get(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };

        // Add shapes for connected directions
        if (state.get(UP)) shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 8, 4, 12, 16, 12));
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 0, 4, 12, 8, 12));
        if (state.get(NORTH)) shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 4, 0, 12, 12, 8));
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 4, 8, 12, 12, 16));
        if (state.get(WEST)) shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 4, 4, 8, 12, 12));
        if (state.get(EAST)) shape = VoxelShapes.union(shape, Block.createCuboidShape(8, 4, 4, 16, 12, 12));


        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    // Waterlogging
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
}