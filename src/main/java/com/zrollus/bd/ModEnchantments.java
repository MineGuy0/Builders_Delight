package com.zrollus.bd;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;

public class ModEnchantments {

    public static final RegistryKey<Enchantment> HAMMERING = of("hammering");
    public static final RegistryKey<Enchantment> ARTHROPEDIC_EFFICIENCY = of("arthropedic_efficiency");
    public static final RegistryKey<Enchantment> AXING = of("axing");
    public static final RegistryKey<Enchantment> SHOVELING = of("shoveling");
    public static final RegistryKey<Enchantment> FLUIDBREAKER = of("fluidbreaker");
    public static final RegistryKey<Enchantment> BANE_OF_THE_END = of("bane_of_the_end");
    public static final RegistryKey<Enchantment> BANE_OF_THE_DARK = of("bane_of_the_dark");
    public static final RegistryKey<Enchantment> BANE_OF_THE_FLESH = of("bane_of_the_flesh");


    private static RegistryKey<Enchantment> of(String name) {
        return RegistryKey.of(
                RegistryKeys.ENCHANTMENT,
                net.minecraft.util.Identifier.of("bd", name)
        );
    }


    public static int getLevel(
            ItemStack stack,
            RegistryWrapper.WrapperLookup registryLookup,
            RegistryKey<Enchantment> key
    ) {
        RegistryEntry<Enchantment> enchantment =
                registryLookup
                        .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                        .getOrThrow(key);

        return EnchantmentHelper.getLevel(enchantment, stack);
    }


    public static void registerModEnchantments() {
        System.out.println("Registering Enchantment Keys for Builder's Delight");
    }
}
