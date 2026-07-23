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
    @Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluidMiningSpeed(BlockState state, PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        // Check if it's ANY fluid (Water or Lava)
        if (!state.getFluidState().isEmpty() && ModEnchantments.getLevel(player.getMainHandStack(), player.getWorld().getRegistryManager(), ModEnchantments.FLUIDBREAKER) > 0) {

            // Match a beacon's normal breaking time. A beacon has hardness 3 and
            // does not require a specific tool, so vanilla advances by 1 / (3 * 30)
            // each tick: 90 ticks, or roughly 4.5 seconds.
            cir.setReturnValue(1.0F / 90.0F);
        }
    }
}
