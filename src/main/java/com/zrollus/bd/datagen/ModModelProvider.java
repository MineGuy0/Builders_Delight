package com.zrollus.bd.datagen;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.block.custom.BulbBlock;
import com.zrollus.bd.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STEEL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.STEEL_GRATE);
        // Define models for lit and unlit states

        Identifier litModel = new Identifier("bd", "block/steel_bulb_block_on");
        Identifier unlitModel = new Identifier("bd", "block/steel_bulb_block_off");

        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(ModBlocks.STEEL_BULB_BLOCK)
                .coordinate(BlockStateModelGenerator.createBooleanModelMap(BulbBlock.LIT, litModel, unlitModel))
        );
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
    }


    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.AETERNIUM_INGOT, Models.GENERATED);

        itemModelGenerator.register(ModItems.BANANA, Models.GENERATED);
        itemModelGenerator.register(ModItems.CHERRY, Models.GENERATED);

        itemModelGenerator.register(ModItems.POKEDOLLAR, Models.HANDHELD);
        itemModelGenerator.register(ModItems.HAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.LIFE_WEAVER_SWORD, Models.HANDHELD);
        itemModelGenerator.register(ModItems.PAXEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.REAPER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.STARDUST_SWORD, Models.HANDHELD);

        itemModelGenerator.registerArmor(((ArmorItem) ModItems.CLOAK));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_HELMET));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_CHESTPLATE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_LEGGINGS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.GALAXY_BOOTS));
    }
}
