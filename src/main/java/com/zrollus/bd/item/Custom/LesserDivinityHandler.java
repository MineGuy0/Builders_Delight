package com.zrollus.bd.item.Custom;

import com.zrollus.bd.item.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;

public class LesserDivinityHandler {

    // Tags to track the player's state
    private static final String TAG_LAUNCHING = "divinity_launching";
    private static final String TAG_EXILED = "divinity_exiled";
    private static final double TARGET_COORD = 7000000.0;

    public static void register() {
        // 1. FATAL DAMAGE TRIGGER
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                // Check if hit is fatal and they are holding the item (Diamond as placeholder)
                if (player.getHealth() - amount <= 0 && isHoldingDivinity(player)) {
                    triggerDivinity(player);
                    return false; // CANCEL DEATH
                }
            }
            return true;
        });

        // 2. THE CONTINUOUS TICK LOOP
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handleStates(player);
            }
        });
    }

    private static boolean isHoldingDivinity(ServerPlayerEntity player) {
        // Replace Items.DIAMOND with your actual Lesser Divinity Item
        return player.getMainHandStack().isOf(ModItems.LESSER_DIVINITY) || player.getOffHandStack().isOf(ModItems.LESSER_DIVINITY);
    }

    private static void triggerDivinity(ServerPlayerEntity player) {
        // Consume the item
        ItemStack stack = player.getMainHandStack().isOf(ModItems.LESSER_DIVINITY) ? player.getMainHandStack() : player.getOffHandStack();
        stack.decrement(1);

        player.setHealth(1.0f); // 0.5 Hearts
        player.addCommandTag(TAG_LAUNCHING);

        // Initial upward boost
        player.addVelocity(0, 5.0, 0);
        player.velocityModified = true;

        player.sendMessage(Text.literal("§cThe Divinity shatters. You are cast out."), false);
    }

    private static void handleStates(ServerPlayerEntity player) {
        if (player.getCommandTags().contains("divinity_launching")) {
            applyLaunchLogic(player);
        } else if (player.getCommandTags().contains("divinity_exiled")) {
            applyExileLogic(player);
        }
    }

    private static void applyLaunchLogic(ServerPlayerEntity player) {
        // 1. Keep them at 0.5 hearts during the "warp"
        player.setHealth(1.0f);

        // 2. Add Blindness and Nausea to mask the chunk loading "stutter"
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0, false, false));

        // 3. Calculate the jump
        Vec3d target = new Vec3d(TARGET_COORD, 350, TARGET_COORD);
        Vec3d direction = target.subtract(player.getPos()).normalize();

        // 10k blocks per tick is supersonic but manageable for the server
        double leapDistance = 10000.0;
        double nextX = player.getX() + (direction.x * leapDistance);
        double nextZ = player.getZ() + (direction.z * leapDistance);

        // 4. Use teleport instead of setVelocity to bypass physics lag
        player.teleport((ServerWorld)player.getWorld(), nextX, 350, nextZ, player.getYaw(), player.getPitch());

        // 5. Particles to show they are "burning up" in atmosphere
        ((ServerWorld)player.getWorld()).spawnParticles(ParticleTypes.SONIC_BOOM,
                player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);

        // 6. Transition to the "Exiled" state once close enough
        if (player.getPos().distanceTo(target) < 15000) {
            player.getCommandTags().remove("divinity_launching");
            player.addCommandTag("divinity_exiled");
            player.sendMessage(Text.literal("§k||§r §cARRIVAL§r §k||"), false);
        }
    }

    private static void applyExileLogic(ServerPlayerEntity player) {
        WorldBorder border = player.getWorld().getWorldBorder();
        boolean isOutside = !border.contains(player.getBlockPos());

        if (isOutside) {
            player.setHealth(1.0f); // Lock health
            // Resistance 255 makes them deathless
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 25, 254, false, false));

            if (player.age % 40 == 0) {
                player.sendMessage(Text.literal("§7Return to the border to regain your mortality..."), true);
            }
        } else {
            // Clean up when they cross back in
            player.getCommandTags().remove("divinity_exiled");
            player.removeStatusEffect(StatusEffects.RESISTANCE);
            player.sendMessage(Text.literal("§aYou have returned."));
        }
    }
}