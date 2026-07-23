package com.zrollus.bd.biome;

import com.zrollus.bd.BuildersDelight;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.sound.MusicType; // Note: MusicType moved packages in some yarn versions, or uses the SoundEvent directly now
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

public class ModBiomes {
    // 1.21 Identifier Change: Use Identifier.of() instead of Identifier.of()
    public static final RegistryKey<Biome> CORRUPTED_WASTES = RegistryKey.of(RegistryKeys.BIOME,
            Identifier.of(BuildersDelight.MOD_ID, "corrupted_wastes"));

    public static void bootstrap(Registerable<Biome> context) {
        var lookup = context.getRegistryLookup(RegistryKeys.PLACED_FEATURE);
        var carverLookup = context.getRegistryLookup(RegistryKeys.CONFIGURED_CARVER);

        BiomeEffects effects = new BiomeEffects.Builder()
                .waterColor(7368816)
                .waterFogColor(6710886)
                .skyColor(10526880)
                .grassColor(8421504)
                .foliageColor(6710886)
                .fogColor(11184810)
                .moodSound(BiomeMoodSound.CAVE)
                .music(MusicType.GAME) // 1.21 holds this natively as a Holder now!
                .build();

        Biome biome = new Biome.Builder()
                .precipitation(true)
                .downfall(0.4f)
                .temperature(0.7f)
                .generationSettings(new GenerationSettings.LookupBackedBuilder(lookup, carverLookup).build())
                .spawnSettings(new SpawnSettings.Builder().build())
                .effects(effects)
                .build();

        context.register(RegistryKey.of(RegistryKeys.BIOME, Identifier.of("bd", "corruption")), biome);
    }

    public static void globalOverworldGeneration(GenerationSettings.LookupBackedBuilder builder) {
        DefaultBiomeFeatures.addLandCarvers(builder);
        DefaultBiomeFeatures.addAmethystGeodes(builder);
        DefaultBiomeFeatures.addDungeons(builder);
        DefaultBiomeFeatures.addMineables(builder);
        DefaultBiomeFeatures.addSprings(builder);
        DefaultBiomeFeatures.addFrozenTopLayer(builder);
    }

    public static Biome testBiome(Registerable<Biome> context) {
        SpawnSettings.Builder spawnBuilder = new SpawnSettings.Builder();

        GenerationSettings.LookupBackedBuilder biomeBuilder =
                new GenerationSettings.LookupBackedBuilder(context.getRegistryLookup(RegistryKeys.PLACED_FEATURE),
                        context.getRegistryLookup(RegistryKeys.CONFIGURED_CARVER));

        globalOverworldGeneration(biomeBuilder);
        DefaultBiomeFeatures.addMossyRocks(biomeBuilder);
        DefaultBiomeFeatures.addDefaultOres(biomeBuilder);
        DefaultBiomeFeatures.addExtraGoldOre(biomeBuilder);

        biomeBuilder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_PLAINS);
        DefaultBiomeFeatures.addForestFlowers(biomeBuilder);
        DefaultBiomeFeatures.addLargeFerns(biomeBuilder);

        DefaultBiomeFeatures.addDefaultMushrooms(biomeBuilder);
        DefaultBiomeFeatures.addDefaultVegetation(biomeBuilder);

        // 1.21 Biome.Builder changes
        return new Biome.Builder()
                .precipitation(true) // Changed from boolean to Enum
                .downfall(0.4f)
                .temperature(0.7f)
                .generationSettings(biomeBuilder.build())
                .spawnSettings(spawnBuilder.build())
                .effects((new BiomeEffects.Builder())
                        .waterColor(7368816)
                        .waterFogColor(6710886)
                        .skyColor(10526880)
                        .grassColor(8421504)
                        .foliageColor(6710886)
                        .fogColor(11184810)
                        .moodSound(BiomeMoodSound.CAVE)
                        .music(MusicType.GAME) // MusicType.GAME is now an un-registered holder wrapper, .getValue() fetches the actual MusicSound object
                        .build())
                .build();
    }
}