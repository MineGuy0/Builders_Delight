package com.zrollus.bd.item.Custom;

import com.zrollus.bd.world.TheBelowWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class RiftShardItem extends Item {
    public RiftShardItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.getRegistryKey().equals(World.NETHER) || player.getY() >= 0) {
            if (!world.isClient) {
                player.sendMessage(Text.literal("The shard remains cold. It only answers from The Below.")
                        .formatted(Formatting.DARK_GRAY), true);
            }
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            if (TheBelowWorld.escape(serverPlayer) && !player.isCreative()) {
                stack.decrement(1);
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
