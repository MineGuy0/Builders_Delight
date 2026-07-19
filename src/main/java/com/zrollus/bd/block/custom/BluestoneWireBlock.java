package com.zrollus.bd.block.custom;

import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import java.util.*;

public class BluestoneWireBlock extends RedstoneWireBlock {
    public static final BooleanProperty POWERED = Properties.POWERED;

    public static final EnumProperty<WireConnection> NORTH = RedstoneWireBlock.WIRE_CONNECTION_NORTH;
    public static final EnumProperty<WireConnection> SOUTH = RedstoneWireBlock.WIRE_CONNECTION_SOUTH;
    public static final EnumProperty<WireConnection> EAST = RedstoneWireBlock.WIRE_CONNECTION_EAST;
    public static final EnumProperty<WireConnection> WEST = RedstoneWireBlock.WIRE_CONNECTION_WEST;

    public BluestoneWireBlock(Settings settings) {
        super(settings.luminance(state -> state.get(POWERED) ? 15 : 0).noCollision().nonOpaque());
        this.setDefaultState(this.getDefaultState().with(POWERED, false)
                .with(NORTH, WireConnection.NONE).with(SOUTH, WireConnection.NONE)
                .with(EAST, WireConnection.NONE).with(WEST, WireConnection.NONE));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return updateConnections(ctx.getWorld(), this.getDefaultState(), ctx.getBlockPos());
    }

    private BlockState updateConnections(WorldAccess world, BlockState state, BlockPos pos) {
        return state.with(NORTH, getRenderConnection(world, pos, Direction.NORTH))
                .with(SOUTH, getRenderConnection(world, pos, Direction.SOUTH))
                .with(EAST, getRenderConnection(world, pos, Direction.EAST))
                .with(WEST, getRenderConnection(world, pos, Direction.WEST));
    }

    private WireConnection getRenderConnection(BlockView world, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.offset(dir);
        if (world.getBlockState(neighborPos).isOf(this)) return WireConnection.SIDE;
        if (world.getBlockState(pos.up()).isSolidBlock(world, pos.up()) && world.getBlockState(neighborPos.up()).isOf(this)) return WireConnection.UP;
        if (world.getBlockState(neighborPos.down()).isOf(this)) return WireConnection.SIDE;
        return WireConnection.NONE;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean hasPower = world.isReceivingRedstonePower(pos) || scanForPower(world, pos);
            if (state.get(POWERED) != hasPower) {
                applyPowerQueue(world, pos, hasPower);
            } else {
                world.setBlockState(pos, updateConnections(world, state, pos), 3);
            }
        }
    }

    private boolean scanForPower(World world, BlockPos start) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (world.isReceivingRedstonePower(pos)) return true;
            for (Direction d : Direction.values()) {
                BlockPos next = pos.offset(d);
                if (world.getBlockState(next).isOf(this) && visited.add(next)) queue.add(next);
            }
            if (visited.size() > 512) break;
        }
        return false;
    }

    private void applyPowerQueue(World world, BlockPos start, boolean powered) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = world.getBlockState(pos);
            if (state.isOf(this)) {
                // Update State and visuals
                BlockState nextState = updateConnections(world, state.with(POWERED, powered), pos);
                world.setBlockState(pos, nextState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);

                // Power neighboring lamps/pistons
                world.updateNeighborsAlways(pos, this);

                for (Direction d : Direction.values()) {
                    BlockPos next = pos.offset(d);
                    if (world.getBlockState(next).isOf(this) && visited.add(next)) queue.add(next);
                }
            }
        }
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) { return true; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }
}