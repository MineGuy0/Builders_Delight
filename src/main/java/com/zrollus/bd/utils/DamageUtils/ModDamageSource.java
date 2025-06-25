package com.zrollus.bd.utils.DamageUtils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.damage.DamageType;

public class ModDamageSource extends DamageSource {
    private final Entity attacker;

    public ModDamageSource(RegistryEntry<DamageType> damageTypeEntry, Entity attacker) {
        super(damageTypeEntry);
        this.attacker = attacker;
    }

    @Override
    public Entity getAttacker() {
        return attacker;
    }

    public boolean isMagic() {
        // This makes the damage bypass potion resistance and magic resistance
        return true;
    }
}