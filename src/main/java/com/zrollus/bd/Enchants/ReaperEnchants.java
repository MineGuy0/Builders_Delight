package com.zrollus.bd.Enchants;
import com.zrollus.bd.item.Custom.HammerItem;
import com.zrollus.bd.item.Custom.PaxelItem;
import com.zrollus.bd.item.Custom.ReaperItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class HammerEnchants extends Enchantment {
    public HammerEnchants() {
        super(Rarity.UNCOMMON, EnchantmentTarget.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if (stack.getItem() instanceof HammerItem) {
            return false;
        }
        if (stack.getItem() instanceof PaxelItem) {
            return false;
        }
        if (stack.getItem() instanceof ReaperItem) {
            return true;
        }

        // Otherwise, reject:
        return false;
    }
    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return true;
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

}
