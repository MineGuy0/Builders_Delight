package com.zrollus.bd.item.Custom;

import com.zrollus.bd.utils.DamageUtils.ModDamageSource;
import com.zrollus.bd.utils.DamageUtils.ModDamageTypes;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LifeWeaverSword extends SwordItem {
    float _maxdamage = 0.9f;

    public LifeWeaverSword(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings, float MaxDamage) {
        super(material, settings.attributeModifiers(SwordItem.createAttributeModifiers(material, attackDamage, attackSpeed)));
        _maxdamage = MaxDamage;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.getWorld().isClient) {
            DamageSource lifeweaverSource = new ModDamageSource(ModDamageTypes.LIFEWEAVER_ENTRY, attacker);
            float trueDamage = target.getMaxHealth() * _maxdamage;
            if (trueDamage != 0) {
                target.setHealth(target.getHealth() - trueDamage);
                target.damage(lifeweaverSource, trueDamage);
            }
            else {
                target.setHealth(target.getMaxHealth());
            }

            // Check if the target is now dead
            if (!target.isAlive() && target.isPlayer()) {
                Text msg = Text.translatable(
                        "message.bd.life_weaver_kill",
                        target.getDisplayName(),
                        attacker.getDisplayName()
                );

                // Send the message to all players on the server
                target.getWorld().getServer().getPlayerManager().broadcast(msg, false);
            }
        }

        stack.damage(1, attacker, net.minecraft.entity.EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.tutorialmod.life_weaver_sword.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
