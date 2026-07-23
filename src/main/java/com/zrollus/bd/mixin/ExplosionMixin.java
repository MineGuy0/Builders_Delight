package com.zrollus.bd.mixin;

import com.zrollus.bd.Lib.libHelpers.ShopSignData;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private World world;

    @Inject(method = "affectWorld", at = @At("HEAD"), require = 0)
    private void onAffectWorld(boolean particles, CallbackInfo ci) {
        // Because of the Access Widener, we can just cast 'this'
        // and access 'affectedBlocks' directly as if it were public!
        Explosion self = (Explosion) (Object) this;

        self.getAffectedBlocks().removeIf(pos -> {
            BlockEntity be = world.getBlockEntity(pos);

            // Protect Shop Signs
            if (be instanceof SignBlockEntity sign && ShopSignData.from(sign) != null) {
                return true;
            }

            // Protect Chests/Containers attached to Shop Signs
            if (be instanceof Inventory) {
                for (Direction dir : Direction.values()) {
                    if (world.getBlockEntity(pos.offset(dir)) instanceof SignBlockEntity s) {
                        if (ShopSignData.from(s) != null) return true;
                    }
                }
            }
            return false;
        });
    }
}