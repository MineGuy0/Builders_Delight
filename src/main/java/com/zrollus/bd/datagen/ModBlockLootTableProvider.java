package com.zrollus.bd.datagen;

import com.zrollus.bd.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryWrapper;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {

    // 1.21.1 FIX: Added the registry lookup parameter for the constructor
    public ModBlockLootTableProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        Field[] fields = ModBlocks.class.getDeclaredFields();

        for (Field field : fields) {
            if (Block.class.isAssignableFrom(field.getType())) {
                try {
                    Block block = (Block) field.get(null);
                    // Standard auto-drop logic remains valid, but ensure
                    // it is registered within the data generation pipeline
                    addDrop(block);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}