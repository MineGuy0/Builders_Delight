package com.zrollus.bd.datagen;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.utils.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {

        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
            .add(ModBlocks.CUT_STEEL_BLOCK)
            .add(ModBlocks.CHISELED_STEEL_BLOCK)
            .add(ModBlocks.STEEL_BLOCK)
            .add(ModBlocks.STEEL_GRATE)
            .add(ModBlocks.STEEL_BULB_BLOCK)
            .add(ModBlocks.CALCITE_STAIRS)
            .add(ModBlocks.CALCITE_SLAB)
            .add(ModBlocks.CALCITE_WALL)
            .add(ModBlocks.CALCITE_BRICKS)
            .add(ModBlocks.CALCITE_BRICK_SLAB)
            .add(ModBlocks.CALCITE_BRICK_WALL)
            .add(ModBlocks.CALCITE_BRICK_STAIRS)
            .add(ModBlocks.POLISHED_CALCITE)
            .add(ModBlocks.POLISHED_CALCITE_SLAB)
            .add(ModBlocks.POLISHED_CALCITE_WALL)
            .add(ModBlocks.POLISHED_CALCITE_STAIRS)
            .add(ModBlocks.TUFF_STAIRS)
            .add(ModBlocks.TUFF_WALL)
            .add(ModBlocks.TUFF_SLAB)
            .add(ModBlocks.TUFF_BRICKS)
            .add(ModBlocks.TUFF_BRICK_SLAB)
            .add(ModBlocks.TUFF_BRICK_STAIRS)
            .add(ModBlocks.TUFF_BRICK_WALL)
            .add(ModBlocks.TUFF_BRICK_WALL)
            .add(ModBlocks.POLISHED_TUFF)
            .add(ModBlocks.POLISHED_TUFF_SLAB)
            .add(ModBlocks.POLISHED_TUFF_STAIRS)
            .add(ModBlocks.POLISHED_TUFF_WALL)
            .add(ModBlocks.SMOOTH_BASALT_SLAB)
            .add(ModBlocks.SMOOTH_BASALT_STAIRS)
            .add(ModBlocks.SMOOTH_BASALT_WALL)
            .add(ModBlocks.SMOOTH_SANDSTONE_WALL)
            .add(ModBlocks.SMOOTH_RED_SANDSTONE_WALL)
            .add(ModBlocks.SMOOTH_QUARTZ_WALL)
            .add(ModBlocks.POLISHED_GRANITE_WALL)
            .add(ModBlocks.POLISHED_DIORITE_WALL)
            .add(ModBlocks.POLISHED_ANDESITE_WALL)
            .add(ModBlocks.BLACK_LAMP)
            .add(ModBlocks.BROWN_LAMP)
            .add(ModBlocks.WHITE_LAMP)
            .add(ModBlocks.GRAY_LAMP)
            .add(ModBlocks.LIGHT_GRAY_LAMP)
            .add(ModBlocks.RED_LAMP)
            .add(ModBlocks.ORANGE_LAMP)
            .add(ModBlocks.YELLOW_LAMP)
            .add(ModBlocks.LIGHT_GREEN_LAMP)
            .add(ModBlocks.GREEN_LAMP)
            .add(ModBlocks.LIGHT_BLUE_LAMP)
            .add(ModBlocks.CYAN_LAMP)
            .add(ModBlocks.BLUE_LAMP)
            .add(ModBlocks.PURPLE_LAMP)
            .add(ModBlocks.MAGENTA_LAMP)
            .add(ModBlocks.PINK_LAMP)
            .add(ModBlocks.FOLLY_LAMP)
            .add(ModBlocks.BLACK_CUSHION)
            .add(ModBlocks.BROWN_CUSHION)
            .add(ModBlocks.WHITE_CUSHION)
            .add(ModBlocks.GRAY_CUSHION)
            .add(ModBlocks.LIGHT_GRAY_CUSHION)
            .add(ModBlocks.RED_CUSHION)
            .add(ModBlocks.ORANGE_CUSHION)
            .add(ModBlocks.YELLOW_CUSHION)
            .add(ModBlocks.LIME_CUSHION)
            .add(ModBlocks.GREEN_CUSHION)
            .add(ModBlocks.LIGHT_BLUE_CUSHION)
            .add(ModBlocks.BLUE_CUSHION)
            .add(ModBlocks.PURPLE_CUSHION)
            .add(ModBlocks.MAGENTA_CUSHION)
            .add(ModBlocks.PINK_CUSHION)
            .add(ModBlocks.KEYCARD_READER_1)
            .add(ModBlocks.KEYCARD_READER_2)
            .add(ModBlocks.KEYCARD_READER_3)
            .add(ModBlocks.KEYCARD_READER_4)
            .add(ModBlocks.KEYCARD_READER_5)
        ;


        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
            .add(ModBlocks.OAK_LOG_PILLAR)
//            .add(ModBlocks.DARK_OAK_LOG_PILLAR)
//            .add(ModBlocks.JUNGLE_LOG_PILLAR)
//            .add(ModBlocks.ACACIA_LOG_PILLAR)
//            .add(ModBlocks.CHERRY_LOG_PILLAR)
//            .add(ModBlocks.BIRCH_LOG_PILLAR)
//            .add(ModBlocks.MANGROVE_LOG_PILLAR)
//            .add(ModBlocks.SPRUCE_LOG_PILLAR)
//            .add(ModBlocks.STRIPPED_ACACIA_LOG_PILLAR)
//            .add(ModBlocks.STRIPPED_BIRCH_LOG_PILLAR)
//            .add(ModBlocks.STRIPPED_CHERRY_LOG_PILLAR)
//            .add(ModBlocks.STRIPPED_SPRUCE_LOG_PILLAR)
//            .add(ModBlocks.STRIPPED_DARK_OAK_LOG_PILLAR)
//            .add(ModBlocks.STRIPPED_JUNGLE_LOG_PILLAR)
//            .add(ModBlocks.STRIPPED_MANGROVE_LOG_PILLAR)
//            .add(ModBlocks.CRIMSON_STEM_PILLAR)
//            .add(ModBlocks.WARPED_STEM_PILLAR)
//            .add(ModBlocks.STRIPPED_CRIMSON_STEM_PILLAR)
//            .add(ModBlocks.STRIPPED_WARPED_STEM_PILLAR)
        ;


        getOrCreateTagBuilder(ModTags.Blocks.PAXEL_MINEABLE)
                .forceAddTag(BlockTags.PICKAXE_MINEABLE)
                .forceAddTag(BlockTags.AXE_MINEABLE)
                .forceAddTag(BlockTags.SHOVEL_MINEABLE);

        getOrCreateTagBuilder(ModTags.Blocks.REAPER_MINEABLE)
                .forceAddTag(BlockTags.PICKAXE_MINEABLE)
                .forceAddTag(BlockTags.AXE_MINEABLE)
                .forceAddTag(BlockTags.SHOVEL_MINEABLE)
                .forceAddTag(BlockTags.HOE_MINEABLE)
        ;

        getOrCreateTagBuilder(BlockTags.WALLS)
                .add(ModBlocks.CALCITE_WALL)
                .add(ModBlocks.POLISHED_ANDESITE_WALL)
                .add(ModBlocks.CALCITE_BRICK_WALL)
                .add(ModBlocks.QUARTZ_WALL)
                .add(ModBlocks.POLISHED_DIORITE_WALL)
                .add(ModBlocks.POLISHED_GRANITE_WALL)
                .add(ModBlocks.POLISHED_CALCITE_WALL)
                .add(ModBlocks.TUFF_WALL)
                .add(ModBlocks.SMOOTH_BASALT_WALL)
                .add(ModBlocks.SMOOTH_QUARTZ_WALL)
                .add(ModBlocks.TUFF_BRICK_WALL)
                .add(ModBlocks.POLISHED_TUFF_WALL)
                .add(ModBlocks.SMOOTH_RED_SANDSTONE_WALL)
                .add(ModBlocks.SMOOTH_SANDSTONE_WALL)
        ;
    }
}