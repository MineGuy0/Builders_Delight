package com.zrollus.bd.Sound;

import com.zrollus.bd.BuildersDelight;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final Identifier SUMMIT_ID = new Identifier(BuildersDelight.MOD_ID, "summit");
    public static SoundEvent SUMMIT = Registry.register(Registries.SOUND_EVENT, SUMMIT_ID, SoundEvent.of(SUMMIT_ID));

    public static void registerSounds() {
        BuildersDelight.LOGGER.info("Registering Sounds for " + BuildersDelight.MOD_ID);
    }
}