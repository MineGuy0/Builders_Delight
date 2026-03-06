package com.zrollus.bd.datagen;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.REAPER, 1)
                .pattern("PAS")
                .pattern("BTH")
                .pattern("BTB")
                .input('P', Items.NETHERITE_PICKAXE)
                .input('A', Items.NETHERITE_AXE)
                .input('S', Items.NETHERITE_SHOVEL)
                .input('H', Items.NETHERITE_HOE)
                .input('T', Items.ECHO_SHARD)
                .input('B', Items.SLIME_BALL)
                .criterion(hasItem(Items.NETHERITE_AXE), conditionsFromItem(Items.NETHERITE_AXE))
                .criterion(hasItem(Items.NETHERITE_PICKAXE), conditionsFromItem(Items.NETHERITE_PICKAXE))
                .criterion(hasItem(Items.NETHERITE_SHOVEL), conditionsFromItem(Items.NETHERITE_SHOVEL))
                .criterion(hasItem(Items.NETHERITE_HOE), conditionsFromItem(Items.NETHERITE_HOE))
                .criterion(hasItem(Items.ECHO_SHARD), conditionsFromItem(Items.ECHO_SHARD))
                .criterion(hasItem(Items.SLIME_BALL), conditionsFromItem(Items.SLIME_BALL))
            .offerTo(exporter, new Identifier(getRecipeName(ModItems.REAPER)));
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.PAXEL, 1)
                .pattern("PAS")
                .pattern("BTB")
                .pattern("BTB")
                .input('P', Items.NETHERITE_PICKAXE)
                .input('A', Items.NETHERITE_AXE)
                .input('S', Items.NETHERITE_SHOVEL)
                .input('T', Items.ECHO_SHARD)
                .input('B', Items.SLIME_BALL)
                .criterion(hasItem(Items.NETHERITE_AXE), conditionsFromItem(Items.NETHERITE_AXE))
                .criterion(hasItem(Items.NETHERITE_PICKAXE), conditionsFromItem(Items.NETHERITE_PICKAXE))
                .criterion(hasItem(Items.NETHERITE_SHOVEL), conditionsFromItem(Items.NETHERITE_SHOVEL))
                .criterion(hasItem(Items.ECHO_SHARD), conditionsFromItem(Items.ECHO_SHARD))
                .criterion(hasItem(Items.SLIME_BALL), conditionsFromItem(Items.SLIME_BALL))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.PAXEL)));
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HAMMER, 1)
                .pattern("NNN")
                .pattern("NTN")
                .pattern(" T ")
                .input('N', Items.NETHERITE_BLOCK)
                .input('T', Items.ECHO_SHARD)
                .criterion(hasItem(Items.NETHERITE_AXE), conditionsFromItem(Items.NETHERITE_AXE))
                .criterion(hasItem(Items.NETHERITE_PICKAXE), conditionsFromItem(Items.NETHERITE_PICKAXE))
                .criterion(hasItem(Items.NETHERITE_SHOVEL), conditionsFromItem(Items.NETHERITE_SHOVEL))
                .criterion(hasItem(Items.ECHO_SHARD), conditionsFromItem(Items.ECHO_SHARD))
                .criterion(hasItem(Items.SLIME_BALL), conditionsFromItem(Items.SLIME_BALL))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.HAMMER)));
ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.OAK_LOG_PILLAR, 3)
                .pattern(" T ")
                .pattern(" T ")
                .pattern(" T ")
                .input('T', Blocks.OAK_LOG)
                .criterion(hasItem(Items.OAK_LOG), conditionsFromItem(Items.OAK_LOG))
                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.OAK_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SPRUCE_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.SPRUCE_LOG)
//        .criterion(hasItem(Items.SPRUCE_LOG), conditionsFromItem(Items.SPRUCE_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.SPRUCE_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.BIRCH_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.BIRCH_LOG)
//        .criterion(hasItem(Items.BIRCH_LOG), conditionsFromItem(Items.BIRCH_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.BIRCH_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.JUNGLE_LOG_PILLAR, 3)
//            .pattern(" T ")
//            .pattern(" T ")
//            .pattern(" T ")
//            .input('T', Blocks.JUNGLE_LOG)
//            .criterion(hasItem(Items.JUNGLE_LOG), conditionsFromItem(Items.JUNGLE_LOG))
//            .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.JUNGLE_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ACACIA_LOG_PILLAR, 3)
//            .pattern(" T ")
//            .pattern(" T ")
//            .pattern(" T ")
//            .input('T', Blocks.ACACIA_LOG)
//            .criterion(hasItem(Items.ACACIA_LOG), conditionsFromItem(Items.ACACIA_LOG))
//            .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.ACACIA_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DARK_OAK_LOG_PILLAR, 3)
//            .pattern(" T ")
//            .pattern(" T ")
//            .pattern(" T ")
//            .input('T', Blocks.DARK_OAK_LOG)
//            .criterion(hasItem(Items.DARK_OAK_LOG), conditionsFromItem(Items.DARK_OAK_LOG))
//            .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.DARK_OAK_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WARPED_STEM_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.WARPED_STEM)
//        .criterion(hasItem(Items.WARPED_STEM), conditionsFromItem(Items.WARPED_STEM))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.WARPED_STEM_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRIMSON_STEM_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.CRIMSON_STEM)
//        .criterion(hasItem(Items.CRIMSON_STEM), conditionsFromItem(Items.CRIMSON_STEM))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.CRIMSON_STEM_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHERRY_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.CHERRY_LOG)
//        .criterion(hasItem(Items.CHERRY_LOG), conditionsFromItem(Items.CHERRY_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.CHERRY_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MANGROVE_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.MANGROVE_LOG)
//        .criterion(hasItem(Items.MANGROVE_LOG), conditionsFromItem(Items.MANGROVE_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.MANGROVE_LOG_PILLAR)));
//
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_OAK_LOG_PILLAR, 3)
//                .pattern(" T ")
//                .pattern(" T ")
//                .pattern(" T ")
//                .input('T', Blocks.STRIPPED_OAK_LOG)
//                .criterion(hasItem(Items.STRIPPED_OAK_LOG), conditionsFromItem(Items.STRIPPED_OAK_LOG))
//                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_OAK_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_SPRUCE_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.STRIPPED_SPRUCE_LOG)
//        .criterion(hasItem(Items.STRIPPED_SPRUCE_LOG), conditionsFromItem(Items.STRIPPED_SPRUCE_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_SPRUCE_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_BIRCH_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.STRIPPED_BIRCH_LOG)
//        .criterion(hasItem(Items.STRIPPED_BIRCH_LOG), conditionsFromItem(Items.STRIPPED_BIRCH_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_BIRCH_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_JUNGLE_LOG_PILLAR, 3)
//            .pattern(" T ")
//            .pattern(" T ")
//            .pattern(" T ")
//            .input('T', Blocks.STRIPPED_JUNGLE_LOG)
//            .criterion(hasItem(Items.STRIPPED_JUNGLE_LOG), conditionsFromItem(Items.STRIPPED_JUNGLE_LOG))
//            .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_JUNGLE_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_ACACIA_LOG_PILLAR, 3)
//            .pattern(" T ")
//            .pattern(" T ")
//            .pattern(" T ")
//            .input('T', Blocks.STRIPPED_ACACIA_LOG)
//            .criterion(hasItem(Items.STRIPPED_ACACIA_LOG), conditionsFromItem(Items.STRIPPED_ACACIA_LOG))
//            .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_ACACIA_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_DARK_OAK_LOG_PILLAR, 3)
//            .pattern(" T ")
//            .pattern(" T ")
//            .pattern(" T ")
//            .input('T', Blocks.STRIPPED_DARK_OAK_LOG)
//            .criterion(hasItem(Items.STRIPPED_DARK_OAK_LOG), conditionsFromItem(Items.STRIPPED_DARK_OAK_LOG))
//            .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_DARK_OAK_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_WARPED_STEM_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.STRIPPED_WARPED_STEM)
//        .criterion(hasItem(Items.STRIPPED_WARPED_STEM), conditionsFromItem(Items.STRIPPED_WARPED_STEM))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_WARPED_STEM_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_CRIMSON_STEM_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.STRIPPED_CRIMSON_STEM)
//        .criterion(hasItem(Items.STRIPPED_CRIMSON_STEM), conditionsFromItem(Items.STRIPPED_CRIMSON_STEM))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_CRIMSON_STEM_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_CHERRY_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.STRIPPED_CHERRY_LOG)
//        .criterion(hasItem(Items.STRIPPED_CHERRY_LOG), conditionsFromItem(Items.STRIPPED_CHERRY_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_CHERRY_LOG_PILLAR)));
//ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STRIPPED_MANGROVE_LOG_PILLAR, 3)
//        .pattern(" T ")
//        .pattern(" T ")
//        .pattern(" T ")
//        .input('T', Blocks.STRIPPED_MANGROVE_LOG)
//        .criterion(hasItem(Items.STRIPPED_MANGROVE_LOG), conditionsFromItem(Items.STRIPPED_MANGROVE_LOG))
//        .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.STRIPPED_MANGROVE_LOG_PILLAR)));
}
}
