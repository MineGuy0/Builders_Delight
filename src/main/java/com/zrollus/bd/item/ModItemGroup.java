package com.zrollus.bd.item;

import com.zrollus.bd.ModBookStacks;
import com.zrollus.bd.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import com.zrollus.bd.BuildersDelight;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class ModItemGroup {
    public static final ItemGroup   BD_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(BuildersDelight.MOD_ID, "bd_group"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.bd"))
                    .icon(() -> new ItemStack(ModItems.AETERNIUM_INGOT)).entries((displayContext, entries) -> {
                        entries.add(ModItems.BANANA);
                        entries.add(ModItems.CHERRY);
                        entries.add(ModItems.AETERNIUM_INGOT);
                        entries.add(ModItems.GALAXY_HELMET);
                        entries.add(ModItems.GALAXY_CHESTPLATE);
                        entries.add(ModItems.GALAXY_LEGGINGS);
                        entries.add(ModItems.GALAXY_BOOTS);
                        entries.add(ModItems.POKEDOLLAR);
                        entries.add(ModItems.PAXEL);
                        entries.add(ModItems.REAPER);
                        entries.add(ModItems.HAMMER);
                        entries.add(ModItems.LIFE_WEAVER_SWORD);
                        entries.add(ModItems.LIFE_ENDER_SWORD);
                        entries.add(ModItems.STARDUST_SWORD);
                        entries.add(ModItems.CLOAK);
                        entries.add(ModBookStacks.getHammeringBook1());
                        entries.add(ModBookStacks.getHammeringBook2());
                        entries.add(ModBookStacks.getHammeringBook3());
                        entries.add(ModBookStacks.getAhtroBook());
                        entries.add(ModBookStacks.getAxingBook());
                        entries.add(ModBookStacks.getShovelingBook());

                        for (int lvl = 1; lvl <= 10; lvl++) {
                            entries.add(EnchantedBookItem.forEnchantment(
                                    new EnchantmentLevelEntry(Enchantments.EFFICIENCY, lvl)
                            ));
                        }
                        entries.add(ModItems.KEYCARD_1);
                        entries.add(ModItems.KEYCARD_2);
                        entries.add(ModItems.KEYCARD_3);
                        entries.add(ModItems.KEYCARD_4);
                        entries.add(ModItems.KEYCARD_5);
                        entries.add(ModBlocks.KEYCARD_READER_1);
                        entries.add(ModBlocks.KEYCARD_READER_2);
                        entries.add(ModBlocks.KEYCARD_READER_3);
                        entries.add(ModBlocks.KEYCARD_READER_4);
                        entries.add(ModBlocks.KEYCARD_READER_5);
                        entries.add(ModBlocks.STEEL_BLOCK);
                        entries.add(ModBlocks.STEEL_BULB_BLOCK);
                        entries.add(ModBlocks.STEEL_GRATE);
                        entries.add(ModBlocks.CUT_STEEL_BLOCK);
                        entries.add(ModBlocks.CHISELED_STEEL_BLOCK);
                        entries.add(ModBlocks.WHITE_LAMP);
                        entries.add(ModBlocks.BLACK_LAMP);
                        entries.add(ModBlocks.GRAY_LAMP);
                        entries.add(ModBlocks.LIGHT_GRAY_LAMP);
                        entries.add(ModBlocks.BROWN_LAMP);
                        entries.add(ModBlocks.FOLLY_LAMP);
                        entries.add(ModBlocks.RED_LAMP);
                        entries.add(ModBlocks.ORANGE_LAMP);
                        entries.add(ModBlocks.YELLOW_LAMP);
                        entries.add(ModBlocks.LIGHT_GREEN_LAMP);
                        entries.add(ModBlocks.GREEN_LAMP);
                        entries.add(ModBlocks.CYAN_LAMP);
                        entries.add(ModBlocks.LIGHT_BLUE_LAMP);
                        entries.add(ModBlocks.BLUE_LAMP);
                        entries.add(ModBlocks.PURPLE_LAMP);
                        entries.add(ModBlocks.MAGENTA_LAMP);
                        entries.add(ModBlocks.PINK_LAMP);

                        entries.add(ModBlocks.OAK_LOG_PILLAR);
                        entries.add(ModBlocks.SPRUCE_LOG_PILLAR);
                        entries.add(ModBlocks.CHERRY_LOG_PILLAR);
                        entries.add(ModBlocks.JUNGLE_LOG_PILLAR);
                        entries.add(ModBlocks.BIRCH_LOG_PILLAR);
                        entries.add(ModBlocks.DARK_OAK_LOG_PILLAR);
                        entries.add(ModBlocks.ACACIA_LOG_PILLAR);
                        entries.add(ModBlocks.CRIMSON_STEM_PILLAR);
                        entries.add(ModBlocks.WARPED_STEM_PILLAR);
                        entries.add(ModBlocks.STRIPPED_OAK_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_SPRUCE_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_CHERRY_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_JUNGLE_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_BIRCH_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_ACACIA_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_DARK_OAK_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_WARPED_STEM_PILLAR);
                        entries.add(ModBlocks.STRIPPED_CRIMSON_STEM_PILLAR);
                        entries.add(ModBlocks.MANGROVE_LOG_PILLAR);
                        entries.add(ModBlocks.STRIPPED_MANGROVE_LOG_PILLAR);

                        entries.add(ModItems.SUMMIT_DISC);
                        entries.add(ModItems.VALUE_DISC);
                        entries.add(ModItems.DOOMCROSSING_DISC);
                        entries.add(ModItems.HELLAGAIN_DISC);
                        entries.add(ModItems.MILIHERO_DISC);
                        entries.add(ModItems.TGD_DISC);
                    }).build());


    public static void registerItemGroups() {
        BuildersDelight.LOGGER.info("Registering Item Groups for " + BuildersDelight.MOD_ID);
    }
}