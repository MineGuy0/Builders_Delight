package com.zrollus.bd.mixin;

import com.zrollus.bd.ModEnchantments;
import com.zrollus.bd.item.ModItems;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyVariable(
            method = "raycast(DFZ)Lnet/minecraft/util/hit/HitResult;",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0 // Prevents Connector from throwing a fatal error if mapping resolution falls back
    )
    private boolean forceIncludeFluids(boolean includeFluids) {
        if ((Object)this instanceof PlayerEntity player) {
            // Check if the item in hand has your custom enchantment
            if (ModEnchantments.getLevel(player.getMainHandStack(), player.getWorld().getRegistryManager(), ModEnchantments.FLUIDBREAKER) > 0) {
                return true;
            }
        }
        return includeFluids;
    }
}