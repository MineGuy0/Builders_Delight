package com.zrollus.bd.datagen;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ModBiomeDataProvider implements DataProvider {
    private final FabricDataOutput output;

    public ModBiomeDataProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        try {
            JsonObject biome = new JsonObject();

            biome.addProperty("temperature", 0.2);
            biome.addProperty("downfall", 0.0);
            biome.addProperty("precipitation", "none");

            JsonObject effects = new JsonObject();
            effects.addProperty("sky_color", 10526880);
            effects.addProperty("fog_color", 11184810);
            effects.addProperty("water_color", 7368816);
            effects.addProperty("water_fog_color", 6710886);
            effects.addProperty("grass_color", 8421504);
            effects.addProperty("foliage_color", 6710886);
            biome.add("effects", effects);

            // ensure directories exist
            Path path = output.getPath().resolve("data/bd/worldgen/biome/corrupted_wastes.json");
            Files.createDirectories(path.getParent());

            DataProvider.writeToPath(writer, biome, path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }


    @Override
    public String getName() {
        return "Corrupted Wastes Biome Data Provider";
    }
}
