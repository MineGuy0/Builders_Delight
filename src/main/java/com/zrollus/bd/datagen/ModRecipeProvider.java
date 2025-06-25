package com.zrollus.bd.datagen;

import com.zrollus.bd.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
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
                .pattern("BKH")
                .pattern("BTB")
                .input('P', Items.NETHERITE_PICKAXE)
                .input('A', Items.NETHERITE_AXE)
                .input('S', Items.NETHERITE_SHOVEL)
                .input('H', Items.NETHERITE_HOE)
                .input('K', Items.KNOWLEDGE_BOOK)
                .input('T', Items.ECHO_SHARD)
                .input('B', Items.SLIME_BALL)
                .criterion(hasItem(Items.NETHERITE_AXE), conditionsFromItem(Items.NETHERITE_AXE))
                .criterion(hasItem(Items.NETHERITE_PICKAXE), conditionsFromItem(Items.NETHERITE_PICKAXE))
                .criterion(hasItem(Items.NETHERITE_SHOVEL), conditionsFromItem(Items.NETHERITE_SHOVEL))
                .criterion(hasItem(Items.NETHERITE_HOE), conditionsFromItem(Items.NETHERITE_HOE))
                .criterion(hasItem(Items.ECHO_SHARD), conditionsFromItem(Items.ECHO_SHARD))
                .criterion(hasItem(Items.KNOWLEDGE_BOOK), conditionsFromItem(Items.KNOWLEDGE_BOOK))
                .criterion(hasItem(Items.SLIME_BALL), conditionsFromItem(Items.SLIME_BALL))
            .offerTo(exporter, new Identifier(getRecipeName(ModItems.REAPER)));
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.PAXEL, 1)
                .pattern("PAS")
                .pattern("BKB")
                .pattern("BTB")
                .input('P', Items.NETHERITE_PICKAXE)
                .input('A', Items.NETHERITE_AXE)
                .input('S', Items.NETHERITE_SHOVEL)
                .input('K', Items.KNOWLEDGE_BOOK)
                .input('T', Items.ECHO_SHARD)
                .input('B', Items.SLIME_BALL)
                .criterion(hasItem(Items.NETHERITE_AXE), conditionsFromItem(Items.NETHERITE_AXE))
                .criterion(hasItem(Items.NETHERITE_PICKAXE), conditionsFromItem(Items.NETHERITE_PICKAXE))
                .criterion(hasItem(Items.NETHERITE_SHOVEL), conditionsFromItem(Items.NETHERITE_SHOVEL))
                .criterion(hasItem(Items.ECHO_SHARD), conditionsFromItem(Items.ECHO_SHARD))
                .criterion(hasItem(Items.KNOWLEDGE_BOOK), conditionsFromItem(Items.KNOWLEDGE_BOOK))
                .criterion(hasItem(Items.SLIME_BALL), conditionsFromItem(Items.SLIME_BALL))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.PAXEL)));
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HAMMER, 1)
                .pattern("NNN")
                .pattern("NKN")
                .pattern(" T ")
                .input('N', Items.NETHERITE_BLOCK)
                .input('K', Items.KNOWLEDGE_BOOK)
                .input('T', Items.ECHO_SHARD)
                .criterion(hasItem(Items.NETHERITE_AXE), conditionsFromItem(Items.NETHERITE_AXE))
                .criterion(hasItem(Items.NETHERITE_PICKAXE), conditionsFromItem(Items.NETHERITE_PICKAXE))
                .criterion(hasItem(Items.NETHERITE_SHOVEL), conditionsFromItem(Items.NETHERITE_SHOVEL))
                .criterion(hasItem(Items.ECHO_SHARD), conditionsFromItem(Items.ECHO_SHARD))
                .criterion(hasItem(Items.KNOWLEDGE_BOOK), conditionsFromItem(Items.KNOWLEDGE_BOOK))
                .criterion(hasItem(Items.SLIME_BALL), conditionsFromItem(Items.SLIME_BALL))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.HAMMER)));
    }
}
