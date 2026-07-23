package com.zrollus.bd.block.custom;

import com.zrollus.bd.item.Custom.KeycardItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class KeycardReaderBlock extends Block {
    public static final BooleanProperty POWERED = BooleanProperty.of("powered");
    private final int requiredLevel;

    public KeycardReaderBlock(Settings settings, int level) {
        super(settings);
        this.requiredLevel = level;
        this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false));
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            ItemStack held = player.getStackInHand(hand);
            if (held.getItem() instanceof KeycardItem keycard && keycard.getLevel() >= requiredLevel) {
                if (!state.get(POWERED)) {
                    BlockState poweredState = state.with(POWERED, true);
                    world.setBlockState(pos, poweredState, Block.NOTIFY_ALL);
                    world.updateNeighborsAlways(pos, this);
                    world.scheduleBlockTick(pos, this, 100); // 5 seconds
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            BlockState unpowered = state.with(POWERED, false);
            world.setBlockState(pos, unpowered, Block.NOTIFY_ALL);
            world.updateNeighborsAlways(pos, this);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }
}