package com.zrollus.bd.mixin;

import com.zrollus.bd.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract float getHealth();
    @Shadow public abstract void setHealth(float health);

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true, require = 0)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // 'this' is the VICTIM
        if ((Object) this instanceof ServerPlayerEntity victim) {

            // Check if the damage is fatal
            if (this.getHealth() - amount <= 0) {

                // Get the ATTACKER from the DamageSource
                if (source.getAttacker() instanceof ServerPlayerEntity attacker) {

                    // Check if the ATTACKER is the one holding the divinity
                    if (attacker.getMainHandStack().isOf(ModItems.LESSER_DIVINITY)) {

                        // 1. Consume the item from the ATTACKER'S hand
                        attacker.getMainHandStack().decrement(1);

                        // 2. Save the VICTIM from death
                        this.setHealth(1.0f); // Lock victim at 0.5 hearts

                        // 3. Cast the VICTIM out
                        victim.addCommandTag("divinity_launching");

                        victim.sendMessage(com.zrollus.bd.Lib.ItemNameCommand.parseLegacyFormatting("&c" + attacker.getName().getString() + " has cast you out of this world!"), false);
                        attacker.sendMessage(com.zrollus.bd.Lib.ItemNameCommand.parseLegacyFormatting("&6The Divinity shatters. Your target is exiled."), false);

                        // 4. Cancel the damage so the victim doesn't die
                        cir.setReturnValue(false);
                        cir.cancel();
                    }
                }
            }
        }
    }
}
