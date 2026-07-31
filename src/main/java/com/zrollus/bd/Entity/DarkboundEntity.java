package com.zrollus.bd.Entity;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class DarkboundEntity extends WitherSkeletonEntity {
    public DarkboundEntity(EntityType<? extends DarkboundEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 75;
    }

    public static DefaultAttributeContainer.Builder createDarkboundAttributes() {
        return AbstractSkeletonEntity.createAbstractSkeletonAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 650.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 35.0)
                .add(EntityAttributes.GENERIC_ARMOR, 30.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 16.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }

    public static boolean canSpawnBelow(EntityType<DarkboundEntity> type, ServerWorldAccess world,
                                        SpawnReason reason, BlockPos pos, Random random) {
        return pos.getY() < 0 && net.minecraft.entity.mob.HostileEntity.canSpawnInDark(
                type, world, reason, pos, random);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                 SpawnReason spawnReason, EntityData entityData) {
        EntityData result = super.initialize(world, difficulty, spawnReason, entityData);
        equipDarkboundGear(world);
        setHealth(getMaxHealth());
        return result;
    }

    private void equipDarkboundGear(ServerWorldAccess world) {
        ItemStack weapon = new ItemStack(random.nextBoolean() ? Items.NETHERITE_SWORD : Items.NETHERITE_AXE);
        enchant(world, weapon, Enchantments.SHARPNESS, 5);
        enchant(world, weapon, Enchantments.FIRE_ASPECT, 2);
        enchant(world, weapon, Enchantments.KNOCKBACK, 2);
        enchant(world, weapon, Enchantments.LOOTING, 3);
        enchant(world, weapon, Enchantments.UNBREAKING, 3);
        enchant(world, weapon, Enchantments.MENDING, 1);
        equipStack(EquipmentSlot.MAINHAND, weapon);

        equipArmor(world, EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        equipArmor(world, EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        equipArmor(world, EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        equipArmor(world, EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot() || slot == EquipmentSlot.MAINHAND) {
                setEquipmentDropChance(slot, 0.01F);
            }
        }
    }

    private void equipArmor(ServerWorldAccess world, EquipmentSlot slot, ItemStack stack) {
        enchant(world, stack, Enchantments.PROTECTION, 4);
        enchant(world, stack, Enchantments.THORNS, 3);
        enchant(world, stack, Enchantments.UNBREAKING, 3);
        enchant(world, stack, Enchantments.MENDING, 1);
        if (slot == EquipmentSlot.HEAD) {
            enchant(world, stack, Enchantments.RESPIRATION, 3);
        } else if (slot == EquipmentSlot.FEET) {
            enchant(world, stack, Enchantments.FEATHER_FALLING, 4);
            enchant(world, stack, Enchantments.DEPTH_STRIDER, 3);
        }
        equipStack(slot, stack);
    }

    private static void enchant(ServerWorldAccess world, ItemStack stack,
                                RegistryKey<Enchantment> enchantment, int level) {
        RegistryEntry<Enchantment> entry = world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(enchantment)
                .orElseThrow();
        stack.addEnchantment(entry, level);
    }
}
