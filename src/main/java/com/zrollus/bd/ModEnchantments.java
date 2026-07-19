package com.zrollus.bd;

import com.zrollus.bd.Enchants.*;
import net.minecraft.enchantment.EfficiencyEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEnchantments {
    public static final Enchantment HAMMERING = register("hammering", new HammeringEnchantment());
    public static final Enchantment ARTHROPEDIC_EFFICIENCY = register("arthropedic_efficiency", new UniversalEnchants());
    public static final Enchantment AXING = register("axing", new HammerEnchants());
    public static final Enchantment SHOVELING = register("shoveling", new HammerEnchants());
    public static final Enchantment FLUIDBREAKER = register("fluidbreaker", new FluidBreakerEnchantment());
    public static final Enchantment EXTENDED_EFFICIENCY;

    static {
        // 1) Look up the vanilla raw ID for efficiency
        int efficiencyRaw = Registries.ENCHANTMENT.getRawId(Enchantments.EFFICIENCY);

        // 2) Create your replacement enchantment (levels up to 10)
        EfficiencyEnchantment extended = new EfficiencyEnchantment(
                Enchantment.Rarity.UNCOMMON,
                new EquipmentSlot[]{ EquipmentSlot.MAINHAND }
        ) {
            @Override public int getMinPower(int level) {
                return 1 + (level - 1) * 10;
            }
            @Override public int getMaxPower(int level) {
                return getMinPower(level) + 15;
            }
            @Override public int getMaxLevel() {
                return 10;
            }
        };

        // 3) Register it at the same slot, passing the ID as a String
        EXTENDED_EFFICIENCY = Registry.register(
                Registries.ENCHANTMENT,
                efficiencyRaw,
                "minecraft:efficiency",   // <-- String here
                extended
        );
    }

    public static void registerEnchantments() {
        System.out.println("Enchants registered");
    }

    private static Enchantment register(String name, Enchantment enchantment) {

        return Registry.register(Registries.ENCHANTMENT, new Identifier("bd", name), enchantment);
    }

    public static void registerModEnchantments() {
        System.out.println("Registering Enchantments for Builder's Delight");
    }
}
