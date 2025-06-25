package com.zrollus.bd;

import com.zrollus.bd.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ModBookStacks {

    public static ItemStack getHammeringBook1() {
        return createHammeringBook(1);
    }

    public static ItemStack getHammeringBook2() {
        return createHammeringBook(2);
    }

    public static ItemStack getHammeringBook3() {
        return createHammeringBook(3);
    }

    public static ItemStack getAhtroBook() {
        return createArthroBook(1);
    }
    public static ItemStack getAxingBook() {
        return createAxingBook(1);
    }
    public static ItemStack getShovelingBook() {
        return createShovelingBook(1);
    }

    private static ItemStack createHammeringBook(int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ModEnchantments.HAMMERING, level));
        return book;
    }
    private static ItemStack createArthroBook(int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ModEnchantments.ARTHROPEDIC_EFFICIENCY, level));
        return book;
    }
    private static ItemStack createAxingBook(int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ModEnchantments.AXING, level));
        return book;
    }
    private static ItemStack createShovelingBook(int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ModEnchantments.SHOVELING, level));
        return book;
    }
}