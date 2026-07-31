package com.zrollus.bd.block.custom;

import com.mojang.serialization.MapCodec;
import com.zrollus.bd.Entity.CollectorBlockEntity;
import com.zrollus.bd.Entity.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class CollectorBlock extends BlockWithEntity {
    public enum Type { ITEM, EXPERIENCE }
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final MapCodec<CollectorBlock> CODEC = createCodec(CollectorBlock::new);

    private final DyeColor color;
    private final Type type;

    public CollectorBlock(Settings settings) {
        this(settings, DyeColor.WHITE, Type.ITEM);
    }

    public CollectorBlock(Settings settings, DyeColor color, Type type) {
        super(settings);
        this.color = color;
        this.type = type;
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(POWERED); }

    @Nullable
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(POWERED,
                context.getWorld().isReceivingRedstonePower(context.getBlockPos()));
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                                  BlockPos sourcePos, boolean notify) {
        if (world.isClient) return;
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_ALL);
            if (world.getBlockEntity(pos) instanceof CollectorBlockEntity collector) {
                collector.onPowerChanged(powered);
            }
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof CollectorBlockEntity collector) {
            player.openHandledScreen(collector);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()
                && world.getBlockEntity(pos) instanceof CollectorBlockEntity collector) {
            ItemScatterer.spawn(world, pos, collector);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CollectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModEntities.COLLECTOR, CollectorBlockEntity::tick);
    }

    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    public DyeColor getColor() { return color; }
    public Type getCollectorType() { return type; }
}
