package com.zrollus.bd.block;

import com.zrollus.bd.BuildersDelight;
import com.zrollus.bd.block.custom.BulbBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block CUT_STEEL_BLOCK = registerBlock("cut_steel_block",
            new Block(FabricBlockSettings.create().mapColor(MapColor.IRON_GRAY).strength(4f).requiresTool()));
    public static final Block CHISELED_STEEL_BLOCK = registerBlock("chiseled_steel_block",
            new Block(FabricBlockSettings.create().mapColor(MapColor.IRON_GRAY).strength(4f).requiresTool()));
    public static final Block STEEL_BLOCK = registerBlock("steel_block",
            new Block(FabricBlockSettings.create().mapColor(MapColor.IRON_GRAY).strength(4f).requiresTool()));
    public static final Block STEEL_GRATE = registerBlock("steel_grate",
            new Block(FabricBlockSettings.create().mapColor(MapColor.IRON_GRAY).strength(4f).requiresTool().nonOpaque()));
    public static final Block STEEL_BULB_BLOCK = registerBlock("steel_bulb_block",
            new BulbBlock(FabricBlockSettings.create().mapColor(MapColor.IRON_GRAY).strength(4f).requiresTool()
                    .luminance(state -> state.get(BulbBlock.LIT) ? 15 : 0)));


    public static final Block WHITE_LAMP = registerBlock("white_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.WHITE).strength(4f).requiresTool().luminance(15)));
    public static final Block BLACK_LAMP = registerBlock("black_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.BLACK).strength(4f).requiresTool().luminance(15)));
    public static final Block LIGHT_GRAY_LAMP = registerBlock("light_gray_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.LIGHT_GRAY).strength(4f).requiresTool().luminance(15)));
    public static final Block GRAY_LAMP = registerBlock("gray_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.GRAY).strength(4f).requiresTool().luminance(15)));
    public static final Block RED_LAMP = registerBlock("red_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.RED).strength(4f).requiresTool().luminance(15)));
    public static final Block ORANGE_LAMP = registerBlock("orange_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.ORANGE).strength(4f).requiresTool().luminance(15)));
    public static final Block YELLOW_LAMP = registerBlock("yellow_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.YELLOW).strength(4f).requiresTool().luminance(15)));
    public static final Block GREEN_LAMP = registerBlock("green_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.GREEN).strength(4f).requiresTool().luminance(15)));
    public static final Block LIGHT_GREEN_LAMP = registerBlock("light_green_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.LIME).strength(4f).requiresTool().luminance(15)));
    public static final Block BLUE_LAMP = registerBlock("blue_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.BLUE).strength(4f).requiresTool().luminance(15)));
    public static final Block LIGHT_BLUE_LAMP = registerBlock("light_blue_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.LIGHT_BLUE).strength(4f).requiresTool().luminance(15)));
    public static final Block CYAN_LAMP = registerBlock("cyan_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.CYAN).strength(4f).requiresTool().luminance(15)));
    public static final Block PINK_LAMP = registerBlock("pink_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.PINK).strength(4f).requiresTool().luminance(15)));
    public static final Block PURPLE_LAMP = registerBlock("purple_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.PURPLE).strength(4f).requiresTool().luminance(15)));
    public static final Block MAGENTA_LAMP = registerBlock("magenta_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.MAGENTA).strength(4f).requiresTool().luminance(15)));
    public static final Block BROWN_LAMP = registerBlock("brown_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.BROWN).strength(4f).requiresTool().luminance(15)));
    public static final Block FOLLY_LAMP = registerBlock("folly_lamp",
            new Block(FabricBlockSettings.create().mapColor(MapColor.BRIGHT_RED).strength(4f).requiresTool().luminance(15)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(BuildersDelight.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(BuildersDelight.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    public static void RegisterModBlocks() {
        BuildersDelight.LOGGER.debug("Registering ModBlocks for " + BuildersDelight.MOD_ID);
    }
}
