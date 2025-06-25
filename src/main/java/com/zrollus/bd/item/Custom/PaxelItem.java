    package com.zrollus.bd.item.Custom;

    import com.zrollus.bd.ModEnchantments;
    import net.minecraft.block.BlockState;
    import net.minecraft.enchantment.EnchantmentHelper;
    import net.minecraft.enchantment.Enchantments;
    import net.minecraft.item.ItemStack;
    import net.minecraft.item.Items;
    import net.minecraft.item.MiningToolItem;
    import net.minecraft.item.ToolMaterial;
    import com.zrollus.bd.utils.ModTags;

    public class PaxelItem extends MiningToolItem {
        public PaxelItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
            super(attackDamage, attackSpeed, material, ModTags.Blocks.PAXEL_MINEABLE, settings);
        }


        @Override
        public boolean isEnchantable(ItemStack stack) {
            return true; // allows enchantment via table
        }

        @Override
        public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
            float base = super.getMiningSpeedMultiplier(stack, state);
            int lvl = EnchantmentHelper.getLevel(ModEnchantments.EXTENDED_EFFICIENCY, stack);
            if (lvl > 5) {
                return base + (lvl - 5) * 2.5f;
            }
            return base;
        }
        @Override
        public boolean isSuitableFor(BlockState state) {
            return Items.DIAMOND_PICKAXE.isSuitableFor(state); // or mimic pickaxe behavior
        }
    }