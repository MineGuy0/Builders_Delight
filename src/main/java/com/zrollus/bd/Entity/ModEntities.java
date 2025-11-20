package com.zrollus.bd.Entity;

import com.zrollus.bd.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.SpawnSettings;

public class ModEntities {

    public static final BlockEntityType<DisplayCaseBlockEntity> DISPLAY_CASE;

    static {

        DISPLAY_CASE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier("bd", "display_case"),
                FabricBlockEntityTypeBuilder.create(DisplayCaseBlockEntity::new, ModBlocks.DISPLAY_CASE).build()
        );
    }

    // Optional: biome spawn registration
    public static void init() {
        // Example: add squirrel spawn to forest biomes
        // BiomeModifications.addSpawn(biome -> biome.getBiomeRegistryEntry().matchesKey(BiomeKeys.FOREST),
        //         SpawnGroup.CREATURE, SQUIRREL, 50, 2, 5);
    }
}
