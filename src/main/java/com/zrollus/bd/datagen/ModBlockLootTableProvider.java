package com.zrollus.bd.datagen;

import com.zrollus.bd.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {
    public ModBlockLootTableProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate() {
        Field[] fields = ModBlocks.class.getDeclaredFields();

        for (Field field : fields) {
            if (Block.class.isAssignableFrom(field.getType())) {
                try {
                    Block block = (Block) field.get(null); // static field, so null
                    addDrop(block); // add every block automatically
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
