package com.zrollus.bd.datagen;

import com.zrollus.bd.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {

    // 1.21.1 Standard ItemTagProvider Constructor
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        // Registers custom armor sets as valid targets for modern Armor Trim templates
        getOrCreateTagBuilder(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.GALAXY_HELMET)
                .add(ModItems.GALAXY_CHESTPLATE)
                .add(ModItems.GALAXY_LEGGINGS)
                .add(ModItems.GALAXY_BOOTS);

        // Registers custom items into Jukebox/Music Disc logic lists
        getOrCreateTagBuilder(ItemTags.CREEPER_DROP_MUSIC_DISCS)
                .add(ModItems.SUMMIT_DISC)
                .add(ModItems.VALUE_DISC)
                .add(ModItems.TGD_DISC)
                .add(ModItems.HELLAGAIN_DISC)
                .add(ModItems.DOOMCROSSING_DISC)
                .add(ModItems.SHUMMIC_DISC)
                .add(ModItems.MILIHERO_DISC);
    }
}