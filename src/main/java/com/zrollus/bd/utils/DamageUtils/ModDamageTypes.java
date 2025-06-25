package com.zrollus.bd.utils.DamageUtils;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import net.minecraft.registry.entry.RegistryEntry;

public class ModDamageTypes {
    public static final Identifier LIFEWEAVER_ID = new Identifier("bd", "lifeweaver");

    // Store the RegistryEntry here
    public static RegistryEntry<DamageType> LIFEWEAVER_ENTRY;

    public static void registerDamageTypes() {
        var damageTypeRegistry = Registries.REGISTRIES.get((RegistryKey) RegistryKeys.DAMAGE_TYPE);

        DamageType lifeweaverType = new DamageType("lifeweaver", 0.0f);  // only 2-arg constructor available

        Registry.register(damageTypeRegistry, LIFEWEAVER_ID, lifeweaverType);

        try {
            LIFEWEAVER_ENTRY = damageTypeRegistry
                    .getEntry((Object) RegistryKey.of(RegistryKeys.DAMAGE_TYPE, LIFEWEAVER_ID));
        } catch (IllegalStateException e) {
            throw new RuntimeException(e); // wrap or handle as needed
        }
    }
}
