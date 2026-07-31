package com.zrollus.bd.mixin;

import com.zrollus.bd.world.TheBelowWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @Shadow
    public abstract boolean matchesInstance(RegistryKey<MultiNoiseBiomeSourceParameterList> key);

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void bd$useBelowBiome(int biomeX, int biomeY, int biomeZ,
                                  MultiNoiseUtil.MultiNoiseSampler noise,
                                  CallbackInfoReturnable<RegistryEntry<Biome>> cir) {
        RegistryEntry<Biome> below = TheBelowWorld.getBiomeEntry();
        if (biomeY < 0 && below != null && matchesInstance(MultiNoiseBiomeSourceParameterLists.NETHER)) {
            cir.setReturnValue(below);
        }
    }
}
