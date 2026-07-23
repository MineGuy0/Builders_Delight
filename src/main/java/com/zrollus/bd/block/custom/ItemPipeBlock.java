package com.zrollus.bd.block.custom;

import com.mojang.serialization.MapCodec;
import com.zrollus.bd.Entity.ItemPipeBlockEntity;
import com.zrollus.bd.Entity.ModEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.ItemScatterer;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public final class ItemPipeBlock extends BlockWithEntity {
    public enum Role { CONNECTOR, INPUT, OUTPUT }

    public static final MapCodec<ItemPipeBlock> CODEC = createCodec(ItemPipeBlock::new);
    public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
    public static final BooleanProperty EAST = ConnectingBlock.EAST;
    public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
    public static final BooleanProperty WEST = ConnectingBlock.WEST;
    public static final BooleanProperty UP = ConnectingBlock.UP;
    public static final BooleanProperty DOWN = ConnectingBlock.DOWN;
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShape CORE = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
    private final DyeColor color;
    private final Role role;

    public ItemPipeBlock(Settings settings) {
        this(settings, DyeColor.WHITE, Role.CONNECTOR);
    }

    public ItemPipeBlock(Settings settings, DyeColor color, Role role) {
        super(settings);
        this.color = color;
        this.role = role;
        setDefaultState(getStateManager().getDefaultState()
                .with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false)
                .with(UP, false).with(DOWN, false).with(POWERED, false));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, POWERED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return withConnections(getDefaultState().with(POWERED,
                context.getWorld().isReceivingRedstonePower(context.getBlockPos())), context.getWorld(), context.getBlockPos());
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return state.with(ConnectingBlock.FACING_PROPERTIES.get(direction), connects(world, neighborPos));
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) return;
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_ALL);
            if (powered) world.scheduleBlockTick(pos, this, 1);
        }
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && state.get(POWERED)) world.scheduleBlockTick(pos, this, 1);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (state.get(POWERED) && world.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) pipe.transferOneStack();
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (role != Role.CONNECTOR && !world.isClient
                && world.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) player.openHandledScreen(pipe);
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()
                && world.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            net.minecraft.item.ItemStack moving = pipe.takeMovingStack();
            if (!moving.isEmpty()) ItemScatterer.spawn(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, moving);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModEntities.ITEM_PIPE, ItemPipeBlockEntity::tick);
    }

    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE;
        if (state.get(NORTH)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5, 5, 0, 11, 11, 5));
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5, 5, 11, 11, 11, 16));
        if (state.get(WEST)) shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 5, 5, 5, 11, 11));
        if (state.get(EAST)) shape = VoxelShapes.union(shape, Block.createCuboidShape(11, 5, 5, 16, 11, 11));
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5, 0, 5, 11, 5, 11));
        if (state.get(UP)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5, 11, 5, 11, 16, 11));
        return shape;
    }

    private BlockState withConnections(BlockState state, WorldAccess world, BlockPos pos) {
        for (Direction direction : Direction.values())
            state = state.with(ConnectingBlock.FACING_PROPERTIES.get(direction), connects(world, pos.offset(direction)));
        return state;
    }

    private boolean connects(WorldAccess world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() instanceof ItemPipeBlock pipe) return pipe.color == color;
        return role != Role.CONNECTOR && world.getBlockEntity(pos) instanceof Inventory;
    }

    public DyeColor getPipeColor() { return color; }
    public Role getRole() { return role; }
    public boolean connectsTo(ItemPipeBlock other) { return other.color == color; }
}
