package com.zrollus.bd.world;

import com.zrollus.bd.BuildersDelight;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.ChunkSection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TheBelowWorld {
    public static final int SURFACE_Y = -48;
    public static final int NETHER_PORTAL_MIN_Y = 5;
    public static final RegistryKey<Biome> BIOME_KEY = RegistryKey.of(
            RegistryKeys.BIOME, Identifier.of(BuildersDelight.MOD_ID, "the_below"));
    private static volatile RegistryEntry<Biome> biomeEntry;
    private static final Map<UUID, BelowDeathLocation> BELOW_DEATH_LOCATIONS = new ConcurrentHashMap<>();

    private record BelowDeathLocation(Vec3d position, float yaw, float pitch) {
    }

    private TheBelowWorld() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                biomeEntry = server.getRegistryManager()
                        .get(RegistryKeys.BIOME)
                        .getEntry(BIOME_KEY)
                        .orElseThrow(() -> new IllegalStateException("Missing biome " + BIOME_KEY.getValue())));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            biomeEntry = null;
            BELOW_DEATH_LOCATIONS.clear();
        });
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            RegistryEntry<Biome> below = biomeEntry;
            if (below == null || !world.getRegistryKey().equals(World.NETHER)) {
                return;
            }
            boolean changed = false;
            ChunkSection[] sections = chunk.getSectionArray();
            for (int index = 0; index < sections.length; index++) {
                if (chunk.sectionIndexToCoord(index) >= 0) {
                    continue;
                }
                sections[index].populateBiomes((x, y, z, noise) -> below,
                        MultiNoiseUtil.createEmptyMultiNoiseSampler(), 0, 0, 0);
                changed = true;
            }
            if (changed) {
                chunk.setNeedsSaving(true);
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            boolean inNether = world.getRegistryKey().equals(World.NETHER);
            boolean inBelow = inNether && (player.getY() < 0 || hit.getBlockPos().getY() < 0);
            if (inBelow && isPlaceable(player.getStackInHand(hand))) {
                if (!world.isClient && hand == Hand.MAIN_HAND) {
                    player.sendMessage(Text.literal("Nothing can be built in The Below.")
                            .formatted(Formatting.DARK_GRAY), true);
                }
                return ActionResult.FAIL;
            }

            if (hand != Hand.MAIN_HAND || !player.isSneaking()
                    || !player.getMainHandStack().isEmpty()
                    || !world.getBlockState(hit.getBlockPos()).isOf(Blocks.BEDROCK)) {
                return ActionResult.PASS;
            }

            boolean entering = inNether && player.getY() >= 0 && hit.getBlockPos().getY() <= 5;
            if (!entering) {
                return ActionResult.PASS;
            }

            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                descend(serverPlayer);
            }
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)
                    || !player.getWorld().getRegistryKey().equals(World.NETHER)
                    || player.getY() >= 0) {
                return;
            }

            BELOW_DEATH_LOCATIONS.put(player.getUuid(),
                    new BelowDeathLocation(player.getPos(), player.getYaw(), player.getPitch()));
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            BelowDeathLocation death = BELOW_DEATH_LOCATIONS.remove(oldPlayer.getUuid());
            boolean oldPlayerWasBelow = oldPlayer.getWorld().getRegistryKey().equals(World.NETHER)
                    && oldPlayer.getY() < 0;
            if (death == null && !oldPlayerWasBelow) return;

            ServerWorld nether = newPlayer.getServer().getWorld(World.NETHER);
            if (nether == null) return;

            Vec3d position = death != null ? death.position() : oldPlayer.getPos();
            float yaw = death != null ? death.yaw() : oldPlayer.getYaw();
            float pitch = death != null ? death.pitch() : oldPlayer.getPitch();
            newPlayer.teleport(nether, position.x, position.y, position.z, yaw, pitch);
            newPlayer.fallDistance = 0;
            newPlayer.sendMessage(Text.literal("Even death cannot release you from The Below.")
                    .formatted(Formatting.DARK_RED), false);
        });
    }

    public static RegistryEntry<Biome> getBiomeEntry() {
        return biomeEntry;
    }

    private static boolean isPlaceable(net.minecraft.item.ItemStack stack) {
        return stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem;
    }

    private static boolean isBreakable(net.minecraft.item.ItemStack stack) {
        return false;
    }

    private static void descend(ServerPlayerEntity player) {
        ServerWorld destination = player.getServerWorld();

        BlockPos landing = findBelowLanding(destination, player.getBlockX(), player.getBlockZ());
        if (landing == null) {
            landing = createBelowLanding(destination, player.getBlockX(), player.getBlockZ());
        }

        player.teleport(destination, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5,
                player.getYaw(), player.getPitch());
        player.fallDistance = 0;
        player.sendMessage(Text.literal("You descend beneath the Nether, into The Below.")
                .formatted(Formatting.DARK_PURPLE), false);
    }

    public static boolean escape(ServerPlayerEntity player) {
        if (!player.getWorld().getRegistryKey().equals(World.NETHER) || player.getY() >= 0) {
            return false;
        }
        BlockPos landing = findUpperLanding(player.getServerWorld(), player.getBlockX(), player.getBlockZ());
        if (landing == null) return false;
        player.teleport(player.getServerWorld(), landing.getX() + 0.5, landing.getY(),
                landing.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.fallDistance = 0;
        player.sendMessage(Text.literal("The rift shard fractures, dragging you back into the Nether.")
                .formatted(Formatting.LIGHT_PURPLE), false);
        return true;
    }

    private static BlockPos findBelowLanding(ServerWorld world, int centerX, int centerZ) {
        for (int radius = 0; radius <= 24; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    for (int y = SURFACE_Y + 17; y >= SURFACE_Y - 2; y--) {
                        BlockPos result = safeAbove(world,
                                new BlockPos(centerX + dx, y, centerZ + dz));
                        if (result != null) return result;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos findUpperLanding(ServerWorld world, int centerX, int centerZ) {
        for (int radius = 0; radius <= 24; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    for (int y = 5; y <= 120; y++) {
                        BlockPos result = safeAbove(world, new BlockPos(centerX + dx, y, centerZ + dz));
                        if (result != null) return result;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos safeAbove(ServerWorld world, BlockPos floor) {
        if (!world.getBlockState(floor).isSolidBlock(world, floor)) return null;
        BlockPos feet = floor.up();
        if (!world.getBlockState(feet).isAir() || !world.getBlockState(feet.up()).isAir()) return null;
        return feet;
    }

    private static BlockPos createBelowLanding(ServerWorld world, int x, int z) {
        BlockPos center = new BlockPos(x, SURFACE_Y + 1, z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(center.add(dx, -1, dz), Blocks.SOUL_SAND.getDefaultState());
                for (int dy = 0; dy <= 2; dy++) {
                    world.setBlockState(center.add(dx, dy, dz), Blocks.AIR.getDefaultState());
                }
            }
        }
        world.setBlockState(center.add(0, 0, 2), Blocks.SOUL_TORCH.getDefaultState());
        return center;
    }
}
