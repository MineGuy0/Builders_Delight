package com.zrollus.bd.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zrollus.bd.item.ModItems;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;


//@Mixin({Entity.class})
//public abstract class InvisibilityCloakEntityMixin {
//    private boolean IsEquipped(PlayerEntity entity) {
//        boolean hasTrinketEquipped = TrinketsApi.getTrinketComponent(entity)
//                .map(component->component.isEquipped(stack->stack.isOf(ModItems.CLOAK)))
//                .orElse(false);
//        if (hasTrinketEquipped) return true;
//        Iterator<ItemStack> itemIterator = getArmorItems().iterator();
//        while (itemIterator.hasNext()) {
//            ItemStack stack = itemIterator.next();
//            if (stack.getItem().equals(ModItems.CLOAK)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    @Shadow public abstract Iterable<ItemStack> getArmorItems();
//
//    @ModifyReturnValue(method="shouldRender(DDD)Z", at=@At("RETURN"))
//
//    private boolean shouldRender(boolean og) {
//        if (!((Entity)(Object)this instanceof OtherClientPlayerEntity playerEntity)) return og;
//        return !IsEquipped(playerEntity) && og;
//    }
//}
//works but try something else instead



@Mixin(Entity.class)
public abstract class InvisibilityCloakEntityMixin {

    @Unique
    private boolean IsEquipped(PlayerEntity entity) {
        boolean hasTrinketEquipped = TrinketsApi.getTrinketComponent(entity)
                .map(component->component.isEquipped(stack->stack.isOf(ModItems.CLOAK)))
                .orElse(false);
        if (hasTrinketEquipped) return true;
        Iterator<ItemStack> itemIterator = getArmorItems().iterator();
        while (itemIterator.hasNext()) {
            ItemStack stack = itemIterator.next();
            if (stack.getItem().equals(ModItems.CLOAK)) {
                return true;
            }
        }
        return false;
    }

    @Shadow
    public abstract Iterable<ItemStack> getArmorItems();

    @Shadow @Nullable public abstract MinecraftServer getServer();

    @ModifyReturnValue(method = "shouldRender(DDD)Z", at = @At("RETURN"))
    private boolean shouldRender(boolean original) {
        if (!((Entity) (Object) this instanceof OtherClientPlayerEntity playerEntity)) return original;

        // Check if the player has the CLOAK equipped
        if (IsEquipped(playerEntity)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                Camera camera = client.gameRenderer.getCamera();
                Vec3d cameraPos = camera.getPos();
                Vec3d cameraLook = getCameraLookDirection(camera);
                Vec3d playerPos = playerEntity.getPos();

                // Vector from camera to player
                Vec3d toPlayer = playerPos.subtract(cameraPos).normalize();

                // Calculate the angle between the camera's look direction and the player's position
                double dotProduct = cameraLook.dotProduct(toPlayer);
                double angleToPlayer = Math.toDegrees(Math.acos(dotProduct)); // in degrees

                // Field of view logic
                double fov = client.options.getFov().getValue(); // Field of View

                // If the player is within the "center" range, return false to hide them
                if (angleToPlayer <= (fov*0.9)/1.15) {
                    return false; // Player is too close to the center, make them invisible
                }
                return original; // Player is near the edge, so make them visible
            }
            return false;
        }
        return original; // Default rendering behavior for other players
    }
    @Unique
    private Vec3d getCameraLookDirection(Camera camera) {
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        // Convert pitch and yaw to radians
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        // Calculate the look direction
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vec3d(x, y, z);
    }
}
