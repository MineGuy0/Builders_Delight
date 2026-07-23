package com.zrollus.bd.mixin;

import com.zrollus.bd.ModEnchantments;
import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidBlock.class)
public abstract class FluidBlockMixin extends Block {
    public FluidBlockMixin(Settings settings) { super(settings); }

    @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true, require = 0)
    private void showWaterHitbox(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityShapeContext esc && esc.getEntity() instanceof PlayerEntity player) {
            if (ModEnchantments.getLevel(player.getMainHandStack(), player.getWorld().getRegistryManager(), ModEnchantments.FLUIDBREAKER) > 0) {
                cir.setReturnValue(VoxelShapes.fullCube());
            }
        }
    }

}
