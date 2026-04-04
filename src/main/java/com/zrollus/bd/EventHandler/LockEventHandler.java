package com.zrollus.bd.EventHandler;

import com.zrollus.bd.Lib.LockManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LockEventHandler {
    public static final Set<UUID> IS_LOCKING = new HashSet<>();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;

            var pos = hitResult.getBlockPos();
            var state = world.getBlockState(pos);
            var server = world.getServer();
            var manager = LockManager.getServerState(server);
            UUID uuid = player.getUuid();

            // --- LOCKING MODE ---
            if (IS_LOCKING.contains(uuid)) {
                // 1. Check if the block is a valid type AND check current ownership
                if (!isLockable(state, world.getBlockEntity(pos), manager, pos, uuid, player.hasPermissionLevel(2))) {
                    // Message is handled inside isLockable for specific feedback
                    IS_LOCKING.remove(uuid);
                    return ActionResult.FAIL;
                }

                // 2. Proceed with toggle (Already validated ownership/permissions in isLockable)
                manager.toggleLock(pos, uuid, server);
                player.sendMessage(Text.literal(manager.isLocked(pos) ? "§aBlock Locked!" : "§eBlock Unlocked!"), true);

                IS_LOCKING.remove(uuid);
                return ActionResult.SUCCESS;
            }

            // --- SECURITY CHECK (Normal Interaction) ---
            if (manager.isLocked(pos)) {
                if (!uuid.equals(manager.getOwner(pos)) && !player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("§cThis block is locked!"), true);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // Handle Breaking (Cleanup)
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return true;
            var manager = LockManager.getServerState(world.getServer());

            if (manager.isLocked(pos)) {
                UUID owner = manager.getOwner(pos);
                if (player.getUuid().equals(owner) || player.hasPermissionLevel(2)) {
                    manager.toggleLock(pos, owner, world.getServer());
                    return true;
                }
                player.sendMessage(Text.literal("§cThis block is locked!"), true);
                return false;
            }
            return true;
        });
    }

    /**
     * Helper to determine if a block is valid for locking AND if the player has right to toggle it.
     */
    private static boolean isLockable(BlockState state, BlockEntity be, LockManager manager, net.minecraft.util.math.BlockPos pos, UUID playerUuid, boolean isAdmin) {
        Block block = state.getBlock();

        // Check 1: Is it a valid block type?
        boolean isValidType = (block instanceof DoorBlock || block instanceof TrapdoorBlock || block instanceof FenceGateBlock || be instanceof Inventory);

        if (!isValidType) {
            // Player tried to lock dirt/stone/etc.
            return false;
        }

        // Check 2: Is it already locked by someone else?
        if (manager.isLocked(pos)) {
            UUID owner = manager.getOwner(pos);
            if (!playerUuid.equals(owner) && !isAdmin) {
                // Block is already locked by another player
                return false;
            }
        }

        return true;
    }
}