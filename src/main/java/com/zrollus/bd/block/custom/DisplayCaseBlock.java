package com.zrollus.bd.block.custom;

import com.zrollus.bd.Entity.DisplayCaseBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DisplayCaseBlock extends Block implements BlockEntityProvider, Waterloggable {

    public static final DirectionProperty FACING = Properties.FACING;
    public static final EnumProperty<BlockFace> FACE = Properties.BLOCK_FACE;
    BlockFace face = BlockFace.WALL;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(0, 13, 0, 16, 16, 16);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 3, 16);
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 3);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 3, 16, 16);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(13, 0, 0, 16, 16, 16);

    public DisplayCaseBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(FACE, face)
                .with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        Direction clickedFace = ctx.getSide();
        PlayerEntity player = ctx.getPlayer();
        FluidState fluidState = world.getFluidState(pos);

        // Determine wall mount
        BlockFace face = BlockFace.WALL;
        if (clickedFace == Direction.UP) face = BlockFace.FLOOR;
        else if (clickedFace == Direction.DOWN) face = BlockFace.CEILING;

        // Determine facing: if vertical click, face the player; otherwise use the clicked horizontal face
        Direction facing;
        if (clickedFace.getAxis().isVertical() && player != null) {
            facing = player.getHorizontalFacing().getOpposite(); // face player
        } else {
            facing = clickedFace; // horizontal side clicked
        }

        return this.getDefaultState()
                .with(FACING, facing)
                .with(FACE, face)
                .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    // 1.21.1 Updated Override Signature (Protected & No Hand Parameter)
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DisplayCaseBlockEntity entity) {

                // We default to the main hand for taking/placing display items
                Hand hand = Hand.MAIN_HAND;
                ItemStack held = player.getStackInHand(hand);
                ItemStack display = entity.getStack();

                if (!held.isEmpty() && display.isEmpty()) {
                    entity.setStack(held.copy());
                    player.setStackInHand(hand, ItemStack.EMPTY);
                    world.updateListeners(pos, state, state, 3);
                    return ActionResult.SUCCESS;
                }
                else if (held.isEmpty() && !display.isEmpty()) {
                    player.setStackInHand(hand, display.copy());
                    entity.setStack(ItemStack.EMPTY);
                    world.updateListeners(pos, state, state, 3);
                    return ActionResult.SUCCESS;
                }
                else if (!held.isEmpty() && !display.isEmpty()) {
                    player.setStackInHand(hand, display.copy());
                    entity.setStack(held.copy()); // Kept copy safe
                    world.updateListeners(pos, state, state, 3);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.CONSUME; // Use CONSUME on client side or pass along default
    }

    // 1.21.1 Updated Override Signature (Protected)
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(FACE)) { // Swapped property name
            case CEILING -> { return DOWN_SHAPE; }
            case FLOOR -> { return UP_SHAPE; }
            case WALL -> {
                return switch (state.get(FACING)) {
                    case NORTH -> NORTH_SHAPE;
                    case EAST -> EAST_SHAPE;
                    case SOUTH -> SOUTH_SHAPE;
                    case WEST -> WEST_SHAPE;
                    default -> NORTH_SHAPE;
                };
            }
        }
        return DOWN_SHAPE;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayCaseBlockEntity(pos, state);
    }
}