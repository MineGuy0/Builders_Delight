package com.zrollus.bd.mixin;

import com.zrollus.bd.world.TheBelowWorld;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.dimension.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow
    @Final
    private ServerWorld world;

    @Inject(method = "method_61028", at = @At("HEAD"), cancellable = true)
    private void bd$ignoreBelowPortals(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (isNetherBelow(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isValidPortalPos", at = @At("HEAD"), cancellable = true)
    private void bd$rejectBelowPortalSite(BlockPos pos, BlockPos.Mutable mutable,
                                          Direction direction, int offset,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (isNetherBelow(pos)) {
            cir.setReturnValue(false);
        }
    }

    private boolean isNetherBelow(BlockPos pos) {
        return world.getRegistryKey().equals(World.NETHER)
                && pos.getY() < TheBelowWorld.NETHER_PORTAL_MIN_Y;
    }
}
