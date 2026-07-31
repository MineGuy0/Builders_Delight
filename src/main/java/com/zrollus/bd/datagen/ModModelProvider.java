package com.zrollus.bd.datagen;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.block.custom.BulbBlock;
import com.zrollus.bd.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.DyeColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STEEL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STEEL_GRATE);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CUT_STEEL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CHISELED_STEEL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.WHITE_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.RED_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.ORANGE_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.YELLOW_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BLACK_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BLUE_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CYAN_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LIGHT_BLUE_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GRAY_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LIGHT_GRAY_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GREEN_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LIGHT_GREEN_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PINK_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PURPLE_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.MAGENTA_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BROWN_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.FOLLY_LAMP);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.KEYCARD_READER_1);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.KEYCARD_READER_2);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.KEYCARD_READER_3);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.KEYCARD_READER_4);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.KEYCARD_READER_5);

        for (DyeColor color : DyeColor.values()) {
            Block itemCollector = ModBlocks.ITEM_COLLECTORS.get(color);
            Block experienceCollector = ModBlocks.EXPERIENCE_COLLECTORS.get(color);
            Block concrete = concrete(color);
            Block wool = wool(color);
            blockStateModelGenerator.registerSingleton(itemCollector, TextureMap.all(concrete), Models.CUBE_ALL);
            blockStateModelGenerator.registerSingleton(experienceCollector, TextureMap.all(wool), Models.CUBE_ALL);
        }

        BlockStateModelGenerator.BlockTexturePool calcitePool = blockStateModelGenerator.registerCubeAllModelTexturePool(Blocks.CALCITE);
        calcitePool.stairs(ModBlocks.CALCITE_STAIRS);
        calcitePool.slab(ModBlocks.CALCITE_SLAB);
        calcitePool.wall(ModBlocks.CALCITE_WALL);

        // NORMAL LOGS
//        blockStateModelGenerator.registerLog(ModBlocks.OAK_LOG_PILLAR);
//
//        blockStateModelGenerator.registerLog(ModBlocks.STRIPPED_OAK_LOG_PILLAR);
//
//        blockStateModelGenerator.registerLog(ModBlocks.SPRUCE_LOG_PILLAR);
//
//        blockStateModelGenerator.registerLog(ModBlocks.STRIPPED_SPRUCE_LOG_PILLAR);

    // --- Wood / Log / Stripped / Planks as cube_all ---
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.OAK_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_OAK_LOG_PILLAR);
//
//         //--- Spruce ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.SPRUCE_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_SPRUCE_LOG_PILLAR);
//         //--- ACACIA ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.ACACIA_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_ACACIA_LOG_PILLAR);
//         //--- Jungle ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.JUNGLE_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_JUNGLE_LOG_PILLAR);
//         //--- dark oak ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.DARK_OAK_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_DARK_OAK_LOG_PILLAR);
//         //--- cherry ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CHERRY_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_CHERRY_LOG_PILLAR);
//         //--- Birch ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BIRCH_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_BIRCH_LOG_PILLAR);
//         //--- Mangrove ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.MANGROVE_LOG_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_MANGROVE_LOG_PILLAR);
//         //--- Warped ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.WARPED_STEM_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_WARPED_STEM_PILLAR);
//         //--- Crimson ---
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CRIMSON_STEM_PILLAR);
//        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STRIPPED_CRIMSON_STEM_PILLAR);
    }

    private static Block concrete(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_CONCRETE;
            case ORANGE -> Blocks.ORANGE_CONCRETE;
            case MAGENTA -> Blocks.MAGENTA_CONCRETE;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE;
            case YELLOW -> Blocks.YELLOW_CONCRETE;
            case LIME -> Blocks.LIME_CONCRETE;
            case PINK -> Blocks.PINK_CONCRETE;
            case GRAY -> Blocks.GRAY_CONCRETE;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE;
            case CYAN -> Blocks.CYAN_CONCRETE;
            case PURPLE -> Blocks.PURPLE_CONCRETE;
            case BLUE -> Blocks.BLUE_CONCRETE;
            case BROWN -> Blocks.BROWN_CONCRETE;
            case GREEN -> Blocks.GREEN_CONCRETE;
            case RED -> Blocks.RED_CONCRETE;
            case BLACK -> Blocks.BLACK_CONCRETE;
        };
    }

    private static Block wool(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_WOOL;
            case ORANGE -> Blocks.ORANGE_WOOL;
            case MAGENTA -> Blocks.MAGENTA_WOOL;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
            case YELLOW -> Blocks.YELLOW_WOOL;
            case LIME -> Blocks.LIME_WOOL;
            case PINK -> Blocks.PINK_WOOL;
            case GRAY -> Blocks.GRAY_WOOL;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
            case CYAN -> Blocks.CYAN_WOOL;
            case PURPLE -> Blocks.PURPLE_WOOL;
            case BLUE -> Blocks.BLUE_WOOL;
            case BROWN -> Blocks.BROWN_WOOL;
            case GREEN -> Blocks.GREEN_WOOL;
            case RED -> Blocks.RED_WOOL;
            case BLACK -> Blocks.BLACK_WOOL;
        };
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.AETERNIUM_INGOT, Models.GENERATED);

        itemModelGenerator.register(ModItems.BANANA, Models.GENERATED);
        itemModelGenerator.register(ModItems.CHERRY, Models.GENERATED);
        itemModelGenerator.register(ModItems.KEYCARD_1, Models.GENERATED);
        itemModelGenerator.register(ModItems.KEYCARD_2, Models.GENERATED);
        itemModelGenerator.register(ModItems.KEYCARD_3, Models.GENERATED);
        itemModelGenerator.register(ModItems.KEYCARD_4, Models.GENERATED);
        itemModelGenerator.register(ModItems.KEYCARD_5, Models.GENERATED);

        itemModelGenerator.register(ModItems.POKEDOLLAR, Models.HANDHELD);
        itemModelGenerator.register(ModItems.HAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.LIFE_WEAVER_SWORD, Models.HANDHELD);
        itemModelGenerator.register(ModItems.PAXEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.REAPER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.STARDUST_SWORD, Models.HANDHELD);
        itemModelGenerator.register(ModItems.SUMMIT_DISC, Models.GENERATED);
        itemModelGenerator.register(ModItems.DOOMCROSSING_DISC, Models.GENERATED);
        itemModelGenerator.register(ModItems.VALUE_DISC, Models.GENERATED);
        itemModelGenerator.register(ModItems.MILIHERO_DISC, Models.GENERATED);
        itemModelGenerator.register(ModItems.HELLAGAIN_DISC, Models.GENERATED);
        itemModelGenerator.register(ModItems.TGD_DISC, Models.GENERATED);
        itemModelGenerator.register(ModItems.SHUMMIC_DISC, Models.GENERATED);

        itemModelGenerator.register(ModItems.CLOAK, Models.GENERATED);
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_HELMET));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_CHESTPLATE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_LEGGINGS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_BOOTS));

    }
}
