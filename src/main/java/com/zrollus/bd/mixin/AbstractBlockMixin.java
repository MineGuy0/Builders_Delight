package com.zrollus.bd.mixin;

import com.zrollus.bd.ModEnchantments;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {
    @Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
    private void fluidMiningSpeed(BlockState state, PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        // Check if it's ANY fluid (Water or Lava)
        if (!state.getFluidState().isEmpty() && EnchantmentHelper.getLevel(ModEnchantments.FLUIDBREAKER, player.getMainHandStack()) > 0) {

            float baseMultiplier = 0.5f; // Adjust this to make it slower (lower = slower)
            float speed = player.getMainHandStack().getMiningSpeedMultiplier(state);

            // If the tool is "suitable" (which your Hammer is), speed will be high.
            // Divide by a larger number (like 160 or 200) to prevent insta-mining.
            float delta = (speed * baseMultiplier) / 160.0f;

            cir.setReturnValue(delta);
        }
    }
}
