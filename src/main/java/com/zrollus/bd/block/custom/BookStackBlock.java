package com.zrollus.bd.block.custom;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class BookStackBlock extends IgnitableBlock {
    // Tracks 1 to 4 books
    public static final IntProperty BOOKS = IntProperty.of("books", 1, 4);

    // Bounding boxes scaled properly for heights
    private static final VoxelShape SHAPE_1 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);
    private static final VoxelShape SHAPE_2 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    private static final VoxelShape SHAPE_3 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);
    private static final VoxelShape SHAPE_4 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public BookStackBlock(Settings settings) {
        // Inherits LIT and handExtinguishable (true) logic
        super(settings, false, true);
        this.setDefaultState(this.getDefaultState().with(BOOKS, 1).with(WATERLOGGED, false));
    }

    // 1.21.1 FIX: Changed visibility to protected
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // FIX: Realigned to use 1, 2, 3, 4 properly matching the heights
        return switch (state.get(BOOKS)) {
            case 1 -> SHAPE_1;
            case 2 -> SHAPE_2;
            case 3 -> SHAPE_3;
            case 4 -> SHAPE_4;
            default -> SHAPE_1;
        };
    }

    // 1.21.1 FIX: Handled the lighting checks & adding books via item use
    @Override
    protected ItemActionResult onUseWithItem(ItemStack itemStack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // First check if IgnitableBlock wants to light the books with flint/fire charge
        ItemActionResult result = super.onUseWithItem(itemStack, state, world, pos, player, hand, hit);
        if (result.isAccepted()) return result;

        // Otherwise, handle adding books to the stack
        // FIX: Changed condition to < 4 to allow stacking up to the max property size
        if (itemStack.isOf(Items.BOOK) && state.get(BOOKS) < 4) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(BOOKS, state.get(BOOKS) + 1), 3);
                if (!player.getAbilities().creativeMode) {
                    itemStack.decrement(1);
                }
            }
            return ItemActionResult.SUCCESS;
        }

        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // 1.21.1 FIX: Handled empty hand actions (like extinguishing the flames)
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Delegates directly to IgnitableBlock's hand-extinguish logic
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state != null ? state.with(BOOKS, 1) : this.getDefaultState();
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return Items.BOOK.getDefaultStack();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder); // Adds LIT and WATERLOGGED
        builder.add(BOOKS);
    }
}
