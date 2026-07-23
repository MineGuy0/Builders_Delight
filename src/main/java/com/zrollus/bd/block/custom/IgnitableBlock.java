package com.zrollus.bd.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class IgnitableBlock extends Block implements Waterloggable {
    public static final BooleanProperty LIT = Properties.LIT;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private final boolean handExtinguishable;

    public IgnitableBlock(Settings settings, boolean litByDefault, boolean handExtinguishable) {
        super(settings);
        this.handExtinguishable = handExtinguishable;
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(LIT, litByDefault)
                .with(WATERLOGGED, false));
    }

    // 1.21.1 FIX: Overriding the new item interaction method
    @Override
    protected ItemActionResult onUseWithItem(ItemStack itemStack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Logic for lighting the block (Flint and Steel or Fire Charge)
        if (!state.get(LIT) && (itemStack.isOf(Items.FLINT_AND_STEEL) || itemStack.isOf(Items.FIRE_CHARGE))) {
            world.playSound(player, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0f, world.getRandom().nextFloat() * 0.4f + 0.8f);

            if (!world.isClient) {
                world.setBlockState(pos, state.with(LIT, true), 11);

                if (!player.getAbilities().creativeMode) {
                    if (itemStack.isOf(Items.FLINT_AND_STEEL)) {
                        // 1.21.1 FIX: Modern item damage tracking structure
                        EquipmentSlot slot = (hand == Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

// Pass it directly into the damage function
                        itemStack.damage(1, player, slot);
                    } else {
                        itemStack.decrement(1);
                    }
                }
            }
            return ItemActionResult.SUCCESS;
        }

        return super.onUseWithItem(itemStack, state, world, pos, player, hand, hit);
    }

    // 1.21.1 FIX: Overriding the empty hand/generic use method
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // If hand is empty (no item interaction matched above)
        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);

        // Logic for extinguishing by hand (if enabled)
        if (state.get(LIT) && handExtinguishable && itemStack.isEmpty()) {
            world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.0f);

            if (!world.isClient) {
                world.setBlockState(pos, state.with(LIT, false), 11);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
    }

    // 1.21.1 FIX: Visibility changed to protected
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, net.minecraft.util.math.Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
            // Automatically extinguish if submerged
            return state.with(LIT, false).with(WATERLOGGED, true);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }

    // 1.21.1 FIX: Visibility changed to protected
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
}