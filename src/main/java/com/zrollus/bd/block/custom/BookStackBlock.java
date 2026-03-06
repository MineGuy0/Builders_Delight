package com.zrollus.bd.block.custom;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BookStackBlock extends IgnitableBlock {
    // Tracks 1 to 4 books (0-3 internal index)
    public static final IntProperty BOOKS = IntProperty.of("books", 1, 4);

    // Bounding boxes that grow with the stack
    private static final VoxelShape SHAPE_0 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);
    private static final VoxelShape SHAPE_1 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    private static final VoxelShape SHAPE_2 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);
    private static final VoxelShape SHAPE_3 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public BookStackBlock(Settings settings) {
        // Inherits LIT and handExtinguishable (true) logic
        super(settings, false, true);
        this.setDefaultState(this.getDefaultState().with(BOOKS, 1).with(WATERLOGGED, false));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(BOOKS)) {
            case 1 -> SHAPE_1;
            case 2 -> SHAPE_2;
            case 3 -> SHAPE_3;
            default -> SHAPE_0;
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // First check if IgnitableBlock wants to light/extinguish the books
        ActionResult result = super.onUse(state, world, pos, player, hand, hit);
        if (result.isAccepted()) return result;

        // Otherwise, handle adding books to the stack
        if (player.getStackInHand(hand).isOf(Items.BOOK) && state.get(BOOKS) < 3) {
            world.setBlockState(pos, state.with(BOOKS, state.get(BOOKS) + 1), 3);
            if (!player.getAbilities().creativeMode) {
                player.getStackInHand(hand).decrement(1);
            }
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Standard placement state with waterlogging check
        return super.getPlacementState(ctx).with(BOOKS, 1);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder); // Adds LIT and WATERLOGGED
        builder.add(BOOKS);
    }
}