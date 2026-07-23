package com.zrollus.bd.mixin;

import com.zrollus.bd.item.Custom.HammerItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"), require = 0)
    private void syncHammerCracking(int entityId, BlockPos pos, int progress, CallbackInfo ci) {
        ServerWorld world = (ServerWorld) (Object) this;

        // We need to find the player who is causing this break info.
        // Minecraft usually uses the Entity ID of the player for the center block.
        if (world.getEntityById(entityId) instanceof ServerPlayerEntity player) {
            ItemStack stack = player.getMainHandStack();

            if (stack.getItem() instanceof HammerItem hammer) {
                // Get the face and radius using your Hammer logic
                Direction face = hammer.getHitFaceFromLook(player);
                int level = com.zrollus.bd.ModEnchantments.getLevel(stack, world.getRegistryManager(), com.zrollus.bd.ModEnchantments.HAMMERING);
                int radius = 1 + level;

                // Calculate the grid
                List<BlockPos> area = HammerItem.calculateDynamicGrid(pos, face, player, stack, radius);

                for (BlockPos p : area) {
                    if (p.equals(pos)) continue; // Skip center

                    // Create a unique seed for this specific block in the grid
                    // so the cracks don't flicker or overwrite the player's ID
                    int fakeId = p.hashCode() + entityId;

                    // Manually call the method for the neighbors (WITHOUT triggering this mixin again)
                    // We use the internal world reference to broadcast the packet
                    world.getPlayers().forEach(nearbyPlayer -> {
                        if (nearbyPlayer.squaredDistanceTo(p.getX(), p.getY(), p.getZ()) < 1024) {
                            nearbyPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket(fakeId, p, progress));
                        }
                    });
                }
            }
        }
    }
}
