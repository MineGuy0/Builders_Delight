package com.zrollus.bd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;            // <— note package
import net.minecraft.block.BlockState;          // <— note package
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(
            method = "getMiningSpeedMultiplier(Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/BlockState;)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void boostExtendedEfficiency(ItemStack stack, BlockState state, CallbackInfoReturnable<Float> cir) {
        int lvl = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
        if (lvl <= 5) return;

        float vanilla = cir.getReturnValueF();
        int fullBonus = lvl * lvl + 1;      // vanilla formula N²+1 for level N
        int baseBonus = 5 * 5 + 1;          // 5²+1 = 26
        cir.setReturnValue(vanilla + (fullBonus - baseBonus));
    }
}
