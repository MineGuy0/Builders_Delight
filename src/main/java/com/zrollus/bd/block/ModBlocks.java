package com.zrollus.bd.block;

import com.zrollus.bd.BuildersDelight;
import com.zrollus.bd.block.custom.*;
import com.zrollus.bd.block.custom.BulbBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.DyeColor;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

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
   public static final Block BOOK_STACK = registerBlock("book_stack",
           new BookStackBlock(FabricBlockSettings.create()
                   .mapColor(MapColor.PALE_YELLOW)
                   .strength(0.5f)
                   .sounds(BlockSoundGroup.WOOD)
                   .nonOpaque()));
   public static final Block BRAZIER = registerBlock("brazier",
           new BrazierBlock(FabricBlockSettings.create()
                   .mapColor(MapColor.IRON_GRAY)
                   .strength(2.0f)
                   .luminance(state -> state.get(Properties.LIT) ? 15 : 0) // Full brightness
                   .nonOpaque()));

   public static final Block SOUL_BRAZIER = registerBlock("soul_brazier",
           new SoulBrazierBlock(FabricBlockSettings.create()
                   .mapColor(MapColor.IRON_GRAY)
                   .strength(2.0f)
                   .luminance(state -> state.get(Properties.LIT) ? 10 : 0) // Soul light is dimmer
                   .nonOpaque()));

    public static final Block CALCITE_STAIRS = registerBlock("calcite_stairs",
            new StairsBlock(Blocks.CALCITE.getDefaultState(), AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block CALCITE_SLAB = registerBlock("calcite_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block CALCITE_WALL = registerBlock("calcite_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(1.5f, 6.0f).requiresTool()));

 public static final Block CALCITE_BRICKS = registerBlock("calcite_bricks",
            new Block(AbstractBlock.Settings.copy(Blocks.CALCITE)));
 public static final Block CALCITE_BRICK_STAIRS = registerBlock("calcite_brick_stairs",
            new StairsBlock(Blocks.CALCITE.getDefaultState(), AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block CALCITE_BRICK_SLAB = registerBlock("calcite_brick_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block CALCITE_BRICK_WALL = registerBlock("calcite_brick_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block POLISHED_CALCITE = registerBlock("polished_calcite",
            new Block(AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block POLISHED_CALCITE_STAIRS = registerBlock("polished_calcite_stairs",
            new StairsBlock(Blocks.CALCITE.getDefaultState(), AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block POLISHED_CALCITE_SLAB = registerBlock("polished_calcite_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block POLISHED_CALCITE_WALL = registerBlock("polished_calcite_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.CALCITE)));

    public static final Block TUFF_STAIRS = registerBlock("tuff_stairs",
            new StairsBlock(Blocks.TUFF.getDefaultState(), AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block TUFF_SLAB = registerBlock("tuff_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block TUFF_WALL = registerBlock("tuff_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block POLISHED_TUFF = registerBlock("polished_tuff",
            new Block(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block POLISHED_TUFF_STAIRS = registerBlock("polished_tuff_stairs",
            new StairsBlock(Blocks.TUFF.getDefaultState(), AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block POLISHED_TUFF_SLAB = registerBlock("polished_tuff_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block POLISHED_TUFF_WALL = registerBlock("polished_tuff_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block TUFF_BRICKS = registerBlock("tuff_bricks",
            new Block(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block TUFF_BRICK_STAIRS = registerBlock("tuff_brick_stairs",
            new StairsBlock(Blocks.TUFF.getDefaultState(), AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block TUFF_BRICK_SLAB = registerBlock("tuff_brick_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block TUFF_BRICK_WALL = registerBlock("tuff_brick_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.TUFF)));

    public static final Block SMOOTH_BASALT_STAIRS = registerBlock("smooth_basalt_stairs",
            new StairsBlock(Blocks.BASALT.getDefaultState(), AbstractBlock.Settings.copy(Blocks.BASALT)));

    public static final Block SMOOTH_BASALT_SLAB = registerBlock("smooth_basalt_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.BASALT)));

    public static final Block SMOOTH_BASALT_WALL = registerBlock("smooth_basalt_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.BASALT)));

    public static final Block SMOOTH_SANDSTONE_WALL = registerBlock("smooth_sandstone_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.SMOOTH_SANDSTONE)));

    public static final Block SMOOTH_RED_SANDSTONE_WALL = registerBlock("smooth_red_sandstone_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.SMOOTH_RED_SANDSTONE)));

    public static final Block POLISHED_ANDESITE_WALL = registerBlock("polished_andesite_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.ANDESITE)));
    public static final Block POLISHED_DIORITE_WALL = registerBlock("polished_diorite_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.DIORITE)));
    public static final Block POLISHED_GRANITE_WALL = registerBlock("polished_granite_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.ANDESITE)));
    public static final Block QUARTZ_WALL = registerBlock("quartz_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.QUARTZ_BLOCK)));
    public static final Block SMOOTH_QUARTZ_WALL = registerBlock("smooth_quartz_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.QUARTZ_BLOCK)));

    public static final Block OAK_LOG_PILLAR = registerBlock("oak_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)));
    public static final Block STRIPPED_OAK_LOG_PILLAR = registerBlock("stripped_oak_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)));
    public static final Block SPRUCE_LOG_PILLAR = registerBlock("spruce_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.SPRUCE_PLANKS)));
    public static final Block STRIPPED_SPRUCE_LOG_PILLAR = registerBlock("stripped_spruce_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.SPRUCE_PLANKS)));
    public static final Block BIRCH_LOG_PILLAR = registerBlock("birch_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.BIRCH_PLANKS)));
    public static final Block STRIPPED_BIRCH_LOG_PILLAR = registerBlock("stripped_birch_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.BIRCH_PLANKS)));
    public static final Block CHERRY_LOG_PILLAR = registerBlock("cherry_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.CHERRY_PLANKS)));
    public static final Block STRIPPED_CHERRY_LOG_PILLAR = registerBlock("stripped_cherry_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.CHERRY_PLANKS)));
    public static final Block DARK_OAK_LOG_PILLAR = registerBlock("dark_oak_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.DARK_OAK_PLANKS)));
    public static final Block STRIPPED_DARK_OAK_LOG_PILLAR = registerBlock("stripped_dark_oak_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.DARK_OAK_PLANKS)));
    public static final Block ACACIA_LOG_PILLAR = registerBlock("acacia_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.ACACIA_PLANKS)));
    public static final Block STRIPPED_ACACIA_LOG_PILLAR = registerBlock("stripped_acacia_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.ACACIA_PLANKS)));
    public static final Block JUNGLE_LOG_PILLAR = registerBlock("jungle_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.JUNGLE_PLANKS)));
    public static final Block STRIPPED_JUNGLE_LOG_PILLAR = registerBlock("stripped_jungle_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.JUNGLE_PLANKS)));
    public static final Block MANGROVE_LOG_PILLAR = registerBlock("mangrove_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.MANGROVE_PLANKS)));
    public static final Block STRIPPED_MANGROVE_LOG_PILLAR = registerBlock("stripped_mangrove_log_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.MANGROVE_PLANKS)));
    public static final Block WARPED_STEM_PILLAR = registerBlock("warped_stem_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.MANGROVE_PLANKS)));
    public static final Block STRIPPED_WARPED_STEM_PILLAR = registerBlock("stripped_warped_stem_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.MANGROVE_PLANKS)));
    public static final Block CRIMSON_STEM_PILLAR = registerBlock("crimson_stem_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.MANGROVE_PLANKS)));
    public static final Block STRIPPED_CRIMSON_STEM_PILLAR = registerBlock("stripped_crimson_stem_pillar",
            new BranchBlock(FabricBlockSettings.copyOf(Blocks.MANGROVE_PLANKS)));

    public static final Block DISPLAY_CASE = registerBlock("display_case",
            new DisplayCaseBlock(FabricBlockSettings.copyOf(Blocks.GLASS)));

    public static final Block ITEM_PIPE = registerBlock("item_pipe",
            createItemPipe(DyeColor.WHITE, ItemPipeBlock.Role.CONNECTOR));

    public static final Map<DyeColor, Block> ITEM_PIPE_CONNECTORS = registerItemPipes("item_pipe_connector", ItemPipeBlock.Role.CONNECTOR);
    public static final Map<DyeColor, Block> ITEM_PIPE_INPUTS = registerItemPipes("item_pipe_input", ItemPipeBlock.Role.INPUT);
    public static final Map<DyeColor, Block> ITEM_PIPE_OUTPUTS = registerItemPipes("item_pipe_output", ItemPipeBlock.Role.OUTPUT);
    public static final Map<DyeColor, Block> ITEM_COLLECTORS = registerCollectors("item_collector", CollectorBlock.Type.ITEM);
    public static final Map<DyeColor, Block> EXPERIENCE_COLLECTORS = registerCollectors("experience_collector", CollectorBlock.Type.EXPERIENCE);

    public static final Block AQUARIUM_GLASS = registerBlock("aquarium_glass",
            new AquariumGlassBlock(FabricBlockSettings.copyOf(Blocks.GLASS)));


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

    public static final Block BLACK_CUSHION = registerBlock("black_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block WHITE_CUSHION = registerBlock("white_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block BLUE_CUSHION = registerBlock("blue_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block CYAN_CUSHION = registerBlock("cyan_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block LIME_CUSHION = registerBlock("lime_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block GREEN_CUSHION = registerBlock("green_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block RED_CUSHION = registerBlock("red_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block GRAY_CUSHION = registerBlock("gray_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block LIGHT_GRAY_CUSHION = registerBlock("light_gray_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block LIGHT_BLUE_CUSHION = registerBlock("light_blue_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block BROWN_CUSHION = registerBlock("brown_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block YELLOW_CUSHION = registerBlock("yellow_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block PURPLE_CUSHION = registerBlock("purple_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block MAGENTA_CUSHION = registerBlock("magenta_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block PINK_CUSHION = registerBlock("pink_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));
    public static final Block ORANGE_CUSHION = registerBlock("orange_cushion",new CushionBlock(FabricBlockSettings.copy(Blocks.BLACK_WOOL)));

    public static final Block KEYCARD_READER_1 = registerBlock("keycard_reader_1",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(2.0f).requiresTool(), 1));
    public static final Block KEYCARD_READER_2 = registerBlock("keycard_reader_2",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(2.0f).requiresTool(), 2));
    public static final Block KEYCARD_READER_3 = registerBlock("keycard_reader_3",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(2.0f).requiresTool(), 3));
    public static final Block KEYCARD_READER_4 = registerBlock("keycard_reader_4",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(2.0f).requiresTool(), 4));
    public static final Block KEYCARD_READER_5 = registerBlock("keycard_reader_5",
            new KeycardReaderBlock(FabricBlockSettings.create().strength(2.0f).requiresTool(), 5));

    public static final Block TOGGLE_TORCH = registerBlock("toggle_torch",
            new LeverBlock(FabricBlockSettings.create().luminance(15)));

   public static final Block BLUESTONE_WIRE = registerBlock("bluestone_wire",
           new BluestoneWireBlock(FabricBlockSettings.create()
                   .mapColor(MapColor.BLUE)
                   .noCollision()
                   .breakInstantly()
                   .nonOpaque() // Essential for thin wire models
                   .pistonBehavior(PistonBehavior.DESTROY)
                   .replaceable() // Allows placing blocks over it
           ));

   private static Block registerBlock(String name, Block block) {
      registerBlockItem(name, block);
      // FIX: Changed "Identifier.of(...)" to "Identifier.of(...)"
      return Registry.register(Registries.BLOCK, Identifier.of(BuildersDelight.MOD_ID, name), block);
   }

   private static Map<DyeColor, Block> registerItemPipes(String suffix, ItemPipeBlock.Role role) {
       EnumMap<DyeColor, Block> pipes = new EnumMap<>(DyeColor.class);
       for (DyeColor color : DyeColor.values()) {
           pipes.put(color, registerBlock(color.getName() + "_" + suffix, createItemPipe(color, role)));
       }
       return Map.copyOf(pipes);
   }

   private static ItemPipeBlock createItemPipe(DyeColor color, ItemPipeBlock.Role role) {
       return new ItemPipeBlock(FabricBlockSettings.create()
               .mapColor(color.getMapColor())
               .strength(1.5F)
               .sounds(BlockSoundGroup.COPPER)
               .nonOpaque(), color, role);
   }

   private static Map<DyeColor, Block> registerCollectors(String suffix, CollectorBlock.Type type) {
       EnumMap<DyeColor, Block> collectors = new EnumMap<>(DyeColor.class);
       for (DyeColor color : DyeColor.values()) {
           collectors.put(color, registerBlock(color.getName() + "_" + suffix,
                   new CollectorBlock(FabricBlockSettings.create()
                           .mapColor(color.getMapColor())
                           .strength(2.5F)
                           .requiresTool()
                           .nonOpaque()
                           .sounds(BlockSoundGroup.METAL), color, type)));
       }
       return Map.copyOf(collectors);
   }

   public static Block[] getItemPipeBlocks() {
       return Stream.concat(Stream.of(ITEM_PIPE), Stream.of(
                       ITEM_PIPE_CONNECTORS.values(), ITEM_PIPE_INPUTS.values(), ITEM_PIPE_OUTPUTS.values())
               .flatMap(java.util.Collection::stream)).toArray(Block[]::new);
   }

   public static Block[] getCollectorBlocks() {
       return Stream.of(ITEM_COLLECTORS.values(), EXPERIENCE_COLLECTORS.values())
               .flatMap(java.util.Collection::stream).toArray(Block[]::new);
   }

   private static Item registerBlockItem(String name, Block block) {
      // FIX: Changed "Identifier.of(...)" to "Identifier.of(...)"
      // FIX: Changed "new Item.Settings()" to "new Item.Settings()"
      return Registry.register(Registries.ITEM, Identifier.of(BuildersDelight.MOD_ID, name),
              new BlockItem(block, new Item.Settings()));
   }

    public static void RegisterModBlocks() {
//        FlammableBlockRegistry.getDefaultInstance().add(STRIPPED_OAK_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(STRIPPED_SPRUCE_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(STRIPPED_BIRCH_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(STRIPPED_JUNGLE_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(STRIPPED_ACACIA_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(STRIPPED_DARK_OAK_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(STRIPPED_MANGROVE_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(OAK_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(SPRUCE_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(BIRCH_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(JUNGLE_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(ACACIA_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(DARK_OAK_LOG_PILLAR, 10, 5);
//        FlammableBlockRegistry.getDefaultInstance().add(MANGROVE_LOG_PILLAR, 10, 5);
//        BuildersDelight.LOGGER.debug("Registering ModBlocks for " + BuildersDelight.MOD_ID);

    }
}
