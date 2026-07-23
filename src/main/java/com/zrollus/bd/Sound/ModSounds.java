package com.zrollus.bd.Sound;

import com.zrollus.bd.BuildersDelight;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent SUMMIT = registerSoundEvent("summit");
    public static final SoundEvent VALUE = registerSoundEvent("value");
    public static final SoundEvent DOOMCROSSING = registerSoundEvent("doomcrossing");
    public static final SoundEvent HELLAGAIN = registerSoundEvent("hellagain");
    public static final SoundEvent HERO = registerSoundEvent("milihero");
    public static final SoundEvent TGD = registerSoundEvent("tgd");
    public static final SoundEvent SHUMMIC = registerSoundEvent("shummic");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(BuildersDelight.MOD_ID, name);
        SoundEvent event = SoundEvent.of(id);
        return Registry.register(Registries.SOUND_EVENT, id, event);
    }

    public static void registerSounds() {
        BuildersDelight.LOGGER.info("Registering Sounds for " + BuildersDelight.MOD_ID);
    }
}