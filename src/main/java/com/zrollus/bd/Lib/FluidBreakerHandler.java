package com.zrollus.bd.Lib;

import com.zrollus.bd.ModEnchantments;
import com.zrollus.bd.item.Custom.HammerItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative mining for fluid blocks. Vanilla can render an outline for
 * fluids, but it does not reliably keep them as a normal mining target. A short
 * client heartbeat lets the server drive the same ten crack stages as a block.
 */
public final class FluidBreakerHandler {
    public static final int BREAK_TICKS = 90; // beacon-equivalent: 3 hardness * 30
    private static final Map<UUID, MiningSession> SESSIONS = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FluidBreakerHandler::tick);
    }

    public static void update(ServerPlayerEntity player, BlockPos pos, boolean mining) {
        MiningSession previous = SESSIONS.get(player.getUuid());
        if (!mining) {
            clear(player, previous);
            return;
        }

        ServerWorld world = player.getServerWorld();
        if (!canMine(player, world, pos)) {
            clear(player, previous);
            return;
        }

        long tick = world.getTime();
        if (previous == null || !previous.pos.equals(pos)
                || previous.world != world) {
            clear(player, previous);
            SESSIONS.put(player.getUuid(), new MiningSession(world, pos.toImmutable(), 0, tick));
        } else {
            previous.lastHeartbeat = tick;
        }
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, MiningSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MiningSession> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            MiningSession session = entry.getValue();

            if (player == null || player.getServerWorld() != session.world
                    || session.world.getTime() - session.lastHeartbeat > 2
                    || !canMine(player, session.world, session.pos)) {
                session.world.setBlockBreakingInfo(player == null ? session.breakerId : player.getId(), session.pos, -1);
                iterator.remove();
                continue;
            }

            session.breakerId = player.getId();
            session.progress++;
            int stage = Math.min(9, (session.progress * 10) / BREAK_TICKS);
            if (stage != session.lastStage) {
                session.world.setBlockBreakingInfo(player.getId(), session.pos, stage);
                session.lastStage = stage;
            }

            if (session.progress >= BREAK_TICKS) {
                BlockState state = session.world.getBlockState(session.pos);
                session.world.setBlockBreakingInfo(player.getId(), session.pos, -1);
                HammerItem.playFluidBreakEffects(session.world, session.pos, state);
                session.world.setBlockState(session.pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                session.world.syncWorldEvent(2001, session.pos, Block.getRawIdFromState(state));
                player.getMainHandStack().damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
                iterator.remove();
            }
        }
    }

    private static boolean canMine(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        ItemStack tool = player.getMainHandStack();
        if (ModEnchantments.getLevel(tool, world.getRegistryManager(), ModEnchantments.FLUIDBREAKER) <= 0
                || world.getBlockState(pos).getFluidState().isEmpty()
                || player.isSpectator()) {
            return false;
        }

        HitResult hit = player.raycast(6.0, 0.0F, true);
        return hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos);
    }

    private static void clear(ServerPlayerEntity player, MiningSession session) {
        if (session != null) {
            session.world.setBlockBreakingInfo(player.getId(), session.pos, -1);
            SESSIONS.remove(player.getUuid());
        }
    }

    private static final class MiningSession {
        private final ServerWorld world;
        private final BlockPos pos;
        private int progress;
        private long lastHeartbeat;
        private int lastStage = -1;
        private int breakerId;

        private MiningSession(ServerWorld world, BlockPos pos, int progress, long lastHeartbeat) {
            this.world = world;
            this.pos = pos;
            this.progress = progress;
            this.lastHeartbeat = lastHeartbeat;
        }
    }

    private FluidBreakerHandler() {}
}
