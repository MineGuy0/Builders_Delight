package com.zrollus.bd.block.custom;

import com.zrollus.bd.Entity.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;


public class CushionBlock extends Block implements Waterloggable {

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty OCCUPIED = BooleanProperty.of("occupied");

    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 9, 16);

    public CushionBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager()
                        .getDefaultState()
                        .with(WATERLOGGED, false)
                        .with(OCCUPIED, false)
        );
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {

        if (state.get(OCCUPIED)) {
            player.sendMessage(Text.literal("Seat occupied"), true);
            return ActionResult.SUCCESS;
        }

        Vec3d sitPos = new Vec3d(
                pos.getX() + 0.5,
                pos.getY() + 0.35,
                pos.getZ() + 0.5
        );

        if (!world.isClient) {
            SeatEntity seat = ModEntities.SEAT.create(world);
            if (seat != null) {
                seat.setCushionPos(pos);
                seat.refreshPositionAndAngles(sitPos.x, sitPos.y, sitPos.z, player.getYaw(), player.getPitch());
                world.spawnEntity(seat);

                if (player.startRiding(seat)) {
                    world.setBlockState(pos, state.with(OCCUPIED, true), 3);
                } else {
                    seat.discard();
                }
            }
        }

        return ActionResult.SUCCESS;
    }


    /** Attempt to seat an entity on the cushion */
    public boolean trySeat(LivingEntity entity, World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CushionBlock)) return false;

        if (entity.getVehicle() != null) return false; // already riding something
        if (state.get(OCCUPIED)) return false;

        Vec3d sitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5);

        // make sure entity is close enough
        if (entity.squaredDistanceTo(sitPos) > 4.0D) return false;

        SeatEntity seat = ModEntities.SEAT.create(world);
        if (seat == null) return false;

        seat.setCushionPos(pos);
        seat.refreshPositionAndAngles(sitPos.x, sitPos.y, sitPos.z, entity.getYaw(), entity.getPitch());
        world.spawnEntity(seat);

        // Try to ride the seat entity first
        boolean ridingStarted = false;
        if (!world.isClient) {
            ridingStarted = entity.startRiding(seat);
        }

        if (ridingStarted) {
            world.setBlockState(pos, state.with(OCCUPIED, true), 3); // mark as occupied
            return true;
        } else {
            seat.discard(); // fail safe
            return false;
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, OCCUPIED);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction dir,
                                                BlockState neighborState,
                                                WorldAccess world,
                                                BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, dir, neighborState, world, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(
                WATERLOGGED,
                ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER
        );
    }
}
