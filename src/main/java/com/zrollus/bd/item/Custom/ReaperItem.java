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
        super(material, ModTags.Blocks.REAPER_MINEABLE,
                settings.attributeModifiers(MiningToolItem.createAttributeModifiers(material, attackDamage, attackSpeed)));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        Block block = state.getBlock();

        if (state.isIn(ModTags.Blocks.REAPER_MINEABLE)) {
            return 15.0f; // fast mining speed for tag blocks
        }

        if (SWORD_EFFECTIVE_BLOCKS.contains(block)) {
            return 15.0f;
        }

        return super.getMiningSpeed(stack, state);
    }
}
