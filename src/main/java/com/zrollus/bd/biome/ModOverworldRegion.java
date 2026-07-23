package com.zrollus.bd.biome;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.sound.MusicType;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

class ModWorldgenProvider extends FabricDynamicRegistryProvider {
    public ModWorldgenProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        var featureLookup = registries.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE);
        var carverLookup = registries.getWrapperOrThrow(RegistryKeys.CONFIGURED_CARVER);

        BiomeEffects effects = new BiomeEffects.Builder()
                .waterColor(7368816)
                .waterFogColor(6710886)
                .skyColor(10526880)
                .grassColor(8421504)
                .foliageColor(6710886)
                .fogColor(11184810)
                .moodSound(BiomeMoodSound.CAVE)
                .music(MusicType.GAME)
                .build();

        Biome biome = new Biome.Builder()
                .precipitation(true)
                .downfall(0.4f)
                .temperature(0.7f)
                .generationSettings(new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup).build())
                .spawnSettings(new SpawnSettings.Builder().build())
                .effects(effects)
                .build();

        // Register statically into the engine pipeline
        entries.add(RegistryKey.of(RegistryKeys.BIOME, Identifier.of("yourmodid", "corrupted_wastes")), biome);
    }

    @Override
    public String getName() {
        return "World Gen";
    }
}