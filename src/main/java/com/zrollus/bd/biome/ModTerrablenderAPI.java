package com.zrollus.bd.biome;

import com.zrollus.bd.BuildersDelight;
import net.minecraft.util.Identifier;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.TerraBlenderApi;

public class ModTerrablenderAPI implements TerraBlenderApi {
    @Override
    public void onTerraBlenderInitialized() {
        Regions.register(new ModOverworldRegion(new Identifier(BuildersDelight.MOD_ID, "overworld"), 4));

    }
}