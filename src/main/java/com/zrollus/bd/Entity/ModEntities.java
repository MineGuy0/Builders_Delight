package com.zrollus.bd.Entity;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.block.custom.SeatEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.Heightmap;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

public class ModEntities {

    public static final BlockEntityType<DisplayCaseBlockEntity> DISPLAY_CASE;
    public static final BlockEntityType<ItemPipeBlockEntity> ITEM_PIPE;
    public static final BlockEntityType<CollectorBlockEntity> COLLECTOR;
    public static final EntityType<SeatEntity> SEAT;
    public static final EntityType<PlayerShopkeeperEntity> PLAYER_SHOPKEEPER;
    public static final EntityType<DarkboundEntity> DARKBOUND;

    static {

        DISPLAY_CASE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("bd", "display_case"),
                FabricBlockEntityTypeBuilder.create(DisplayCaseBlockEntity::new, ModBlocks.DISPLAY_CASE).build()
        );

        ITEM_PIPE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("bd", "item_pipe"),
                FabricBlockEntityTypeBuilder.create(ItemPipeBlockEntity::new, ModBlocks.getItemPipeBlocks()).build()
        );

        COLLECTOR = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("bd", "collector"),
                FabricBlockEntityTypeBuilder.create(CollectorBlockEntity::new, ModBlocks.getCollectorBlocks()).build()
        );

        SEAT = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of("bd", "seat"),
                FabricEntityTypeBuilder.create(SpawnGroup.MISC, SeatEntity::new)
                        .dimensions(EntityDimensions.fixed(0.001f, 0.001f)) // tiny but non-zero
                        .trackRangeBlocks(64)
                        .build()
        );

        PLAYER_SHOPKEEPER = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of("bd", "player_shopkeeper"),
                FabricEntityTypeBuilder.create(SpawnGroup.MISC, PlayerShopkeeperEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6F, 1.8F))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(2)
                        .build()
        );

        DARKBOUND = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of("bd", "darkbound"),
                FabricEntityTypeBuilder.<DarkboundEntity>createMob()
                        .spawnGroup(SpawnGroup.MONSTER)
                        .entityFactory(DarkboundEntity::new)
                        .spawnRestriction(SpawnLocationTypes.ON_GROUND,
                                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                                DarkboundEntity::canSpawnBelow)
                        .dimensions(EntityDimensions.fixed(0.7F, 2.4F))
                        .trackRangeBlocks(96)
                        .trackedUpdateRate(2)
                        .build()
        );
    }

    // Optional: biome spawn registration
    public static void init() {
        FabricDefaultAttributeRegistry.register(PLAYER_SHOPKEEPER, MobEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(DARKBOUND, DarkboundEntity.createDarkboundAttributes());
        BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether(),
                SpawnGroup.MONSTER, DARKBOUND, 18, 1, 2);
        // Example: add squirrel spawn to forest biomes
        // BiomeModifications.addSpawn(biome -> biome.getBiomeRegistryEntry().matchesKey(BiomeKeys.FOREST),
        //         SpawnGroup.CREATURE, SQUIRREL, 50, 2, 5);
    }
}
