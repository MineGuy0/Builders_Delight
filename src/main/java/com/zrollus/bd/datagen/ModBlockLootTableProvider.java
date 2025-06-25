package com.zrollus.bd.datagen;

import com.zrollus.bd.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {
    public ModBlockLootTableProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate() {
        addDrop(ModBlocks.CUT_STEEL_BLOCK);
        addDrop(ModBlocks.STEEL_BULB_BLOCK);
        addDrop(ModBlocks.STEEL_BLOCK);
        addDrop(ModBlocks.STEEL_GRATE);
        addDrop(ModBlocks.CHISELED_STEEL_BLOCK);
        addDrop(ModBlocks.WHITE_LAMP);
        addDrop(ModBlocks.BLUE_LAMP);
        addDrop(ModBlocks.BROWN_LAMP);
        addDrop(ModBlocks.BLACK_LAMP);
        addDrop(ModBlocks.RED_LAMP);
        addDrop(ModBlocks.ORANGE_LAMP);
        addDrop(ModBlocks.YELLOW_LAMP);
        addDrop(ModBlocks.CYAN_LAMP);
        addDrop(ModBlocks.LIGHT_BLUE_LAMP);
        addDrop(ModBlocks.GREEN_LAMP);
        addDrop(ModBlocks.LIGHT_GREEN_LAMP);
        addDrop(ModBlocks.PURPLE_LAMP);
        addDrop(ModBlocks.PINK_LAMP);
        addDrop(ModBlocks.MAGENTA_LAMP);
        addDrop(ModBlocks.FOLLY_LAMP);
        addDrop(ModBlocks.LIGHT_GRAY_LAMP);
        addDrop(ModBlocks.GRAY_LAMP);
    }
}
