package com.zrollus.bd.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.Identifier;

public class ModArmorEffects {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (hasFullStardustArmor(player)) {
                        applyEffects(player);
                    } else {
                        removeSetBonus(player);
                    }
                }
            }
        });
    }

    private static boolean hasFullStardustArmor(ServerPlayerEntity player) {
        return player.getInventory().armor.get(3).getItem() == ModItems.GALAXY_HELMET &&
                player.getInventory().armor.get(2).getItem() == ModItems.GALAXY_CHESTPLATE &&
                player.getInventory().armor.get(1).getItem() == ModItems.GALAXY_LEGGINGS &&
                player.getInventory().armor.get(0).getItem() == ModItems.GALAXY_BOOTS;
    }
    private static final Identifier HEALTH_BOOST_ID = Identifier.of("bd", "stardust_health_boost");
    private static void applyEffects(ServerPlayerEntity player) {
        // Speed effect (with buffer)
        StatusEffectInstance speed = player.getStatusEffect(StatusEffects.SPEED);
        if (speed == null || speed.getDuration() < 10) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 210, 5, false, false));
        }

        // Max health boost
        var attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        boolean hasModifier = attr.getModifiers().stream()
                .anyMatch(mod -> mod.id().equals(HEALTH_BOOST_ID));

        if (!hasModifier) {
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    HEALTH_BOOST_ID,
                    61.0, // +61 HP = 30.5 hearts
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
            attr.addPersistentModifier(modifier);
        }
    }

    private static void removeSetBonus(ServerPlayerEntity player) {
        // Clean up if they remove the armor
        player.removeStatusEffect(StatusEffects.SPEED);

        var attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        attr.getModifiers().stream()
                .filter(mod -> mod.id().equals(HEALTH_BOOST_ID))
                .findFirst()
                .ifPresent(attr::removeModifier);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

    }
}
