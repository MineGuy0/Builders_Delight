package com.zrollus.bd.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zrollus.bd.item.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hides remote players wearing the cloak while they are inside the viewer's field of view. */
@Mixin(Entity.class)
public abstract class InvisibilityCloakEntityMixin {

    @ModifyReturnValue(
            method = "shouldRender(DDD)Z",
            at = @At("RETURN"),
            require = 0 // <--- CRITICAL for MixinExtras + Connector compatibility
    )
    private boolean bd$hideCloakedPlayer(boolean original) {
        if (!original || !((Object) this instanceof OtherClientPlayerEntity player)) return original;

        // The server synchronizes this tracked flag for /vanish. Suppress the
        // entire entity renderer so there is no translucent body, armor, item,
        // shadow, nameplate, or particle silhouette.
        if (player.isInvisible()) return false;
        if (!player.getOffHandStack().isOf(ModItems.CLOAK)) return original;

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d toPlayer = player.getPos().subtract(camera.getPos());
        if (toPlayer.lengthSquared() < 1.0E-6) return false;
        double dot = Math.max(-1.0, Math.min(1.0, cameraDirection(camera).dotProduct(toPlayer.normalize())));
        double angle = Math.toDegrees(Math.acos(dot));
        return angle > client.options.getFov().getValue() * 0.9 / 1.15;
    }

    private static Vec3d cameraDirection(Camera camera) {
        double pitch = Math.toRadians(camera.getPitch());
        double yaw = Math.toRadians(camera.getYaw());
        return new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }
}