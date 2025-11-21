package com.zrollus.bd.datagen;

import com.zrollus.bd.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }



    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        getOrCreateTagBuilder(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.GALAXY_HELMET, ModItems.GALAXY_CHESTPLATE, ModItems.GALAXY_LEGGINGS, ModItems.GALAXY_BOOTS);

        getOrCreateTagBuilder(ItemTags.MUSIC_DISCS)
                .add(ModItems.SUMMIT_DISC)
                .add(ModItems.VALUE_DISC)
                .add(ModItems.TGD_DISC)
                .add(ModItems.HELLAGAIN_DISC)
                .add(ModItems.DOOMCROSSING_DISC)
                .add(ModItems.SHUMMIC_DISC)
                .add(ModItems.MILIHERO_DISC);

    }
}
