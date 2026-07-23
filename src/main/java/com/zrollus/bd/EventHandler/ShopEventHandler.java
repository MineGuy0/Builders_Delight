package com.zrollus.bd.EventHandler;

import com.zrollus.bd.Lib.MessageLib;
import com.zrollus.bd.Lib.ShopProcessor;
import com.zrollus.bd.Lib.libHelpers.AdminStateRegistry;
import com.zrollus.bd.Lib.libHelpers.ShopSignData;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.zrollus.bd.Lib.MessageLib.sendShopInfo;

public class ShopEventHandler {
    private static final Map<UUID, Long> adminBreakAttempts = new HashMap<>();

    public static void register() {
        // USE BLOCK (Buying)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof SignBlockEntity sign) {
                ShopSignData shop = ShopSignData.from(sign);
                if (shop == null) return ActionResult.PASS;

                if (shop.isAdminShop) {
                    ShopProcessor.processBuy((ServerPlayerEntity) player, shop, null);
                } else {
                    Inventory chest = findAttachedInventory(world, pos);
                    if (chest != null) ShopProcessor.processBuy((ServerPlayerEntity) player, shop, chest);
                    else player.sendMessage(com.zrollus.bd.Lib.ItemNameCommand.parseLegacyFormatting("&6Shop &7» &cNo chest found!"), true);
                }
                return ActionResult.SUCCESS;
            }

            if (be instanceof Inventory inventory) {
                for (Direction dir : Direction.values()) {
                    BlockPos sidePos = pos.offset(dir);
                    if (world.getBlockEntity(sidePos) instanceof SignBlockEntity sideSign) {
                        ShopSignData shop = ShopSignData.from(sideSign);

                        if (shop != null) {
                            boolean isOwner = String.valueOf(player.getName()).equalsIgnoreCase(shop.owner);

                            // New logic: If not owner, block access AND show the cool info
                            // Even OPs are blocked here unless they are in 'AdminBypassMode'
                            if (!isOwner && !AdminStateRegistry.BYPASS_MODE.contains(player.getUuid())) {
                                sendShopInfo((ServerPlayerEntity) player, shop, inventory);
                                return ActionResult.FAIL;
                            }
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });

        // ATTACK BLOCK (Selling & Protection)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient) return ActionResult.PASS;
            BlockEntity be = world.getBlockEntity(pos);
            SignBlockEntity targetSign = null;
            ShopSignData shop = null;

            if (be instanceof SignBlockEntity s) {
                shop = ShopSignData.from(s);
                if (shop != null) targetSign = s;
            } else if (be instanceof Inventory) {
                for (Direction dir : Direction.values()) {
                    if (world.getBlockEntity(pos.offset(dir)) instanceof SignBlockEntity s2) {
                        ShopSignData data = ShopSignData.from(s2);
                        if (data != null) { targetSign = s2; shop = data; break; }
                    }
                }
            }

            if (targetSign == null || shop == null) return ActionResult.PASS;

            if (player.isSneaking() && (String.valueOf(player.getName()).equalsIgnoreCase(shop.owner) || player.hasPermissionLevel(2))) {
                long now = System.currentTimeMillis();
                if (now - adminBreakAttempts.getOrDefault(player.getUuid(), 0L) < 5000) {
                    adminBreakAttempts.remove(player.getUuid());
                    removeHologram(world, pos);
                    return ActionResult.PASS;
                } else {
                    adminBreakAttempts.put(player.getUuid(), now);
                    player.sendMessage(com.zrollus.bd.Lib.ItemNameCommand.parseLegacyFormatting("&6Shop &7» &cPress again to break."), true);
                    return ActionResult.FAIL;
                }
            }

            if (shop.sellPrice > 0) {
                if (shop.isAdminShop) ShopProcessor.processSell((ServerPlayerEntity) player, shop, null);
                else {
                    Inventory chest = findAttachedInventory(world, targetSign.getPos());
                    if (chest != null) ShopProcessor.processSell((ServerPlayerEntity) player, shop, chest);
                }
            }

            world.updateListeners(targetSign.getPos(), world.getBlockState(targetSign.getPos()), world.getBlockState(targetSign.getPos()), 3);
            if (player instanceof ServerPlayerEntity sp) sp.networkHandler.sendPacket(BlockEntityUpdateS2CPacket.create(targetSign));
            return ActionResult.FAIL;
        });
    }

    private static void removeHologram(net.minecraft.world.World world, BlockPos pos) {
        if (world.isClient) return;

        // We define a very tight box exactly where the hologram floats (above the sign)
        // Adjust the '1.5' if your summon offset was different
        Box searchBox = new Box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0
        );

        world.getEntitiesByClass(ArmorStandEntity.class, searchBox, (entity) ->
                entity.isInvisible() && entity.isMarker()
        ).forEach(Entity::discard);
    }

    private static Inventory findAttachedInventory(World world, BlockPos signPos) {
        BlockState state = world.getBlockState(signPos);
        Direction dir = state.contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING)
                ? state.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING).getOpposite() : Direction.DOWN;

        if (world.getBlockEntity(signPos.offset(dir)) instanceof Inventory inv) return inv;
        for (Direction d : Direction.values()) {
            if (world.getBlockEntity(signPos.offset(d)) instanceof Inventory inv) return inv;
        }
        return null;
    }
}
