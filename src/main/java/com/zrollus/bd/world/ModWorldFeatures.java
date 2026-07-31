package com.zrollus.bd.world;

import com.zrollus.bd.BuildersDelight;
import com.zrollus.bd.world.feature.DarkVillageFeature;
import com.zrollus.bd.world.feature.DeadTreeFeature;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

public final class ModWorldFeatures {
    public static final Feature<DefaultFeatureConfig> DEAD_TREE =
            new DeadTreeFeature(DefaultFeatureConfig.CODEC);
    public static final Feature<DefaultFeatureConfig> DARK_VILLAGE =
            new DarkVillageFeature(DefaultFeatureConfig.CODEC);
    public static final RegistryKey<PlacedFeature> DEAD_TREE_PLACED = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE, Identifier.of(BuildersDelight.MOD_ID, "dead_tree"));
    public static final RegistryKey<PlacedFeature> DARK_VILLAGE_PLACED = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE, Identifier.of(BuildersDelight.MOD_ID, "dark_village"));

    private ModWorldFeatures() {
    }

    public static void register() {
        Registry.register(Registries.FEATURE, Identifier.of(BuildersDelight.MOD_ID, "dead_tree"), DEAD_TREE);
        Registry.register(Registries.FEATURE, Identifier.of(BuildersDelight.MOD_ID, "dark_village"), DARK_VILLAGE);
        BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(),
                GenerationStep.Feature.SURFACE_STRUCTURES, DARK_VILLAGE_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(),
                GenerationStep.Feature.VEGETAL_DECORATION, DEAD_TREE_PLACED);
    }
}
