package com.zrollus.bd.item.Custom;

import com.zrollus.bd.utils.DamageUtils.ModDamageSource;
import com.zrollus.bd.utils.DamageUtils.ModDamageTypes;
import net.minecraft.client.item.TooltipContext;
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
    public LifeWeaverSword(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.getWorld().isClient) {
            DamageSource lifeweaverSource = new ModDamageSource(ModDamageTypes.LIFEWEAVER_ENTRY, attacker);
            float trueDamage = target.getMaxHealth() * 0.9f;
            target.setHealth(target.getHealth() - trueDamage);
            target.damage(lifeweaverSource, trueDamage);
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

        stack.damage(1, attacker, e -> e.sendToolBreakStatus(attacker.getActiveHand()));
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tutorialmod.life_weaver_sword.tooltip"));
        tooltip.add(Text.translatable("tooltip.tutorialmod.life_weaver_sword.tooltip2"));
        super.appendTooltip(stack, world, tooltip, context);
    }
}