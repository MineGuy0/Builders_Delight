package com.zrollus.bd.mixin;

import com.zrollus.bd.ModEnchantments;
import com.zrollus.bd.item.ModItems;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyVariable(method = "raycast", at = @At("HEAD"), argsOnly = true)
    private boolean forceIncludeFluids(boolean includeFluids) {
        if ((Object)this instanceof PlayerEntity player) {
            // Check if the item in hand has your custom enchantment
            if (EnchantmentHelper.getLevel(ModEnchantments.FLUIDBREAKER, player.getMainHandStack()) > 0) {
                return true;
            }
        }
        return includeFluids;
    }
}