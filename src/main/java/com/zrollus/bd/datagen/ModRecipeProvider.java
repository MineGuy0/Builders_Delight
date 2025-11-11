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
    }
}
