package com.zrollus.bd;

import com.zrollus.bd.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKeys;

public class ModBookStacks {

    public static ItemStack getHammeringBook1(RegistryWrapper.WrapperLookup lookup) {
        return createBook(lookup, ModEnchantments.HAMMERING, 1);
    }

    public static ItemStack getHammeringBook2(RegistryWrapper.WrapperLookup lookup) {
        return createBook(lookup, ModEnchantments.HAMMERING, 2);
    }

    public static ItemStack getHammeringBook3(RegistryWrapper.WrapperLookup lookup) {
        return createBook(lookup, ModEnchantments.HAMMERING, 3);
    }

    public static ItemStack getAhtroBook(RegistryWrapper.WrapperLookup lookup) {
        return createBook(lookup, ModEnchantments.ARTHROPEDIC_EFFICIENCY, 1);
    }
    public static ItemStack getAxingBook(RegistryWrapper.WrapperLookup lookup) {
        return createBook(lookup, ModEnchantments.AXING, 1);
    }
    public static ItemStack getShovelingBook(RegistryWrapper.WrapperLookup lookup) {
        return createBook(lookup, ModEnchantments.SHOVELING, 1);
    }

    private static ItemStack createBook(RegistryWrapper.WrapperLookup lookup, net.minecraft.registry.RegistryKey<Enchantment> key, int level) {
        return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(
                lookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(key), level));
    }
}
