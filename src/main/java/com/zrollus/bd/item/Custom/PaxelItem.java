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
            super(material, ModTags.Blocks.PAXEL_MINEABLE,
                    settings.attributeModifiers(MiningToolItem.createAttributeModifiers(material, attackDamage, attackSpeed)));
        }


        @Override
        public boolean isEnchantable(ItemStack stack) {
            return true; // allows enchantment via table
        }

        @Override
        public float getMiningSpeed(ItemStack stack, BlockState state) {
            return super.getMiningSpeed(stack, state);
        }
    }
