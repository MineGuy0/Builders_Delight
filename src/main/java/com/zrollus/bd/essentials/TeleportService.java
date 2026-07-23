package com.zrollus.bd.essentials;

import com.zrollus.bd.Lib.LocationStorageLib;
import com.zrollus.bd.Lib.ModConfigHelper;
import com.zrollus.bd.Lib.PermissionCompat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class TeleportService {
    private static final Map<UUID, Long> LAST_TELEPORT = new java.util.concurrent.ConcurrentHashMap<>();

    private TeleportService() {}

    public static boolean teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z,
                                   float yaw, float pitch, boolean findSafeLocation, boolean saveBack) {
        long now = System.currentTimeMillis();
        int cooldown = ModConfigHelper.get().teleportCooldownSeconds;
        if (cooldown > 0 && !PermissionCompat.check(player, "teleport.cooldown.bypass", player.hasPermissionLevel(2))) {
            long remaining = LAST_TELEPORT.getOrDefault(player.getUuid(), 0L) + cooldown * 1000L - now;
            if (remaining > 0) {
                player.sendMessage(Text.literal("You can teleport again in " + ((remaining + 999) / 1000) + " seconds.").formatted(Formatting.RED));
                return false;
            }
        }

        Vec3d destination = findSafeLocation ? safeDestination(world, x, y, z) : new Vec3d(x, y, z);
        if (destination == null) {
            player.sendMessage(Text.literal("No safe teleport destination was found.").formatted(Formatting.RED));
            return false;
        }
        Vec3d start = player.getPos();
        int warmup = ModConfigHelper.get().teleportWarmupSeconds;
        Runnable perform = () -> player.getServer().execute(() -> {
            if (player.isRemoved()) return;
            if (warmup > 0 && player.getPos().squaredDistanceTo(start) > .04) {
                player.sendMessage(Text.literal("Teleport cancelled because you moved.").formatted(Formatting.RED));
                return;
            }
            if (saveBack) LocationStorageLib.saveBackLocation(player);
            player.teleport(world, destination.x, destination.y, destination.z, yaw, pitch);
            LAST_TELEPORT.put(player.getUuid(), System.currentTimeMillis());
            protect(player);
        });
        if (warmup > 0 && !PermissionCompat.check(player, "teleport.warmup.bypass", player.hasPermissionLevel(2))) {
            player.sendMessage(Text.literal("Teleporting in " + warmup + " seconds. Do not move or take damage.").formatted(Formatting.GOLD));
            CompletableFuture.delayedExecutor(warmup, TimeUnit.SECONDS).execute(perform);
        } else perform.run();
        return true;
    }

    private static void protect(ServerPlayerEntity player) {
        int seconds = ModConfigHelper.get().teleportInvulnerabilitySeconds;
        if (seconds <= 0) return;
        player.addCommandTag("bd_teleport_protected");
        CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS).execute(() -> player.getServer().execute(() ->
                player.removeCommandTag("bd_teleport_protected")));
    }

    private static Vec3d safeDestination(ServerWorld world, double x, double y, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int requested = Math.clamp((int) Math.floor(y), world.getBottomY() + 1, world.getTopY() - 2);
        for (int distance = 0; distance < 32; distance++) {
            for (int sign : new int[]{1, -1}) {
                int by = requested + distance * sign;
                if (by <= world.getBottomY() || by >= world.getTopY() - 1) continue;
                BlockPos feet = new BlockPos(bx, by, bz);
                if (safe(world, feet)) return new Vec3d(bx + .5, by, bz + .5);
            }
        }
        int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        BlockPos feet = new BlockPos(bx, surface, bz);
        return safe(world, feet) ? new Vec3d(bx + .5, surface, bz + .5) : null;
    }

    private static boolean safe(ServerWorld world, BlockPos feet) {
        BlockPos floor = feet.down();
        return world.getBlockState(floor).isSolidBlock(world, floor)
                && world.getFluidState(floor).isEmpty()
                && world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()
                && world.getFluidState(feet).isEmpty() && world.getFluidState(feet.up()).isEmpty();
    }
}
