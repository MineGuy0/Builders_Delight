package com.zrollus.bd.block.custom;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class PillarBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    // Shapes: same dimensions, rotated depending on axis
    private static final VoxelShape SHAPE_Y = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape SHAPE_X = Block.createCuboidShape(0.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape SHAPE_Z = Block.createCuboidShape(4.0, 4.0, 0.0, 12.0, 12.0, 16.0);

    public PillarBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.Y));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return changeRotation(state, rotation);
    }

    public static BlockState changeRotation(BlockState state, BlockRotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch (state.get(AXIS)) {
                    case X:
                        return state.with(AXIS, Direction.Axis.Z);
                    case Z:
                        return state.with(AXIS, Direction.Axis.X);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(AXIS, ctx.getSide().getAxis());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }
}
