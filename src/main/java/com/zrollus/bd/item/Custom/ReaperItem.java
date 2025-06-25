package com.zrollus.bd.item.Custom;

import com.zrollus.bd.ModEnchantments;
import com.zrollus.bd.utils.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;

import java.util.Set;

public class ReaperItem extends MiningToolItem {
    private static final Set<Block> SWORD_EFFECTIVE_BLOCKS = Set.of(
            Blocks.COBWEB,
            Blocks.BAMBOO,
            Blocks.BEACON,
            Blocks.VINE,
            Blocks.GLOW_LICHEN
            // Add more if needed
    );

    public ReaperItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(attackDamage, attackSpeed, material, ModTags.Blocks.REAPER_MINEABLE, settings);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        Block block = state.getBlock();

        // Check if the block is in the REAPER_MINEABLE tag
        if (state.isIn(ModTags.Blocks.REAPER_MINEABLE)) {
            return true;
        }

        // Check if block is in SWORD_EFFECTIVE_BLOCKS (like cobwebs)
        // For this method, we do not have access to ItemStack to check enchantment,
        // so just return true here or false depending on desired behavior
        if (SWORD_EFFECTIVE_BLOCKS.contains(block)) {
            return true; // or false if you want to enforce enchantment only in other method
        }

        return false;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        Block block = state.getBlock();

        if (state.isIn(ModTags.Blocks.REAPER_MINEABLE)) {
            return 15.0f; // fast mining speed for tag blocks
        }

        if (SWORD_EFFECTIVE_BLOCKS.contains(block)) {
            // Check enchantment on the stack
            if (EnchantmentHelper.getLevel(ModEnchantments.ARTHROPEDIC_EFFICIENCY, stack) > 0) {
                return 15.0f;
            }
            return 1.0f;
        }

        return super.getMiningSpeedMultiplier(stack, state);
    }
}