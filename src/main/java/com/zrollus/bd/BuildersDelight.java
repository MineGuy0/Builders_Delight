package com.zrollus.bd;


import com.zrollus.bd.Entity.ModEntities;
import com.zrollus.bd.Lib.*;
import com.zrollus.bd.Lib.libHelpers.ShopSignData;
import com.zrollus.bd.Sound.ModSounds;
import com.zrollus.bd.item.Custom.LesserDivinityHandler;
import com.zrollus.bd.item.ModArmorEffects;
import net.fabricmc.api.ModInitializer;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.item.ModItems;
import com.zrollus.bd.item.ModItemGroup;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuildersDelight implements ModInitializer {
	public static final String MOD_ID = "bd";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Wait until the server has been ticking for 5 seconds before
			// we even think about touching the waystones file.
			new Thread(() -> {
				try {
					Thread.sleep(5000);
					// Now it's safe to run your logic or log your message
					LOGGER.info("BD Mod: Safe startup window reached.");
					DynamicIdMapper mapper = DynamicIdMapper.getServerState(server);

					// 2. Run the auto-indexer
					mapper.generateAllIds();
				} catch (InterruptedException ignored) {}
			}).start();
		});

		ModConfigHelper.load();
		ModSounds.registerSounds();
		ModItemGroup.registerItemGroups();
		ModBlocks.RegisterModBlocks();
		ModEntities.init();
		ModItems.registerModItems();
		ModEnchantments.registerModEnchantments();
		ModArmorEffects.register();
		ModNetworking.init();
		ModCommandManager.register();
		LesserDivinityHandler.register();

		registerEvents();



		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayerEntity player) {
				LocationStorageLib.saveBackLocation(player);
				player.sendMessage(Text.literal("You Died at X:").append(String.valueOf(Math.round(player.getX()))).append(" Y:").append(String.valueOf(Math.round(player.getY()))).append(" Z:").append(String.valueOf(Math.round(player.getZ()))).append(". Type /back to return.").formatted(Formatting.GRAY));
			}
		});
	}

	private static final Map<UUID, Long> adminBreakAttempts = new HashMap<>();

	public static void registerEvents() {
		// --- BUY LOGIC (Right Click) ---
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;

			BlockEntity be = world.getBlockEntity(hitResult.getBlockPos());
			if (be instanceof SignBlockEntity sign) {
				ShopSignData shop = ShopSignData.from(sign);

				if (shop == null) return ActionResult.PASS; // Not a shop, let Minecraft handle it

				// If we are here, it IS a shop.
				if (shop.isAdminShop) {
					ShopProcessor.processBuy((ServerPlayerEntity) player, shop, null);
				} else {
					Inventory chest = findAttachedInventory(world, hitResult.getBlockPos());
					if (chest != null) {
						ShopProcessor.processBuy((ServerPlayerEntity) player, shop, chest);
					} else {
						player.sendMessage(Text.literal("§6Shop » §cNo chest found!"), true);
					}
				}
				return ActionResult.SUCCESS; // Prevent sign editing
			}
			return ActionResult.PASS;
		});

		// --- SELL & PROTECT (Left Click) ---
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClient) return ActionResult.PASS;

			BlockEntity be = world.getBlockEntity(pos);
			SignBlockEntity targetSign = null;
			ShopSignData shop = null;

			// 1. Identify if we are hitting a shop or a chest belonging to a shop
			if (be instanceof SignBlockEntity s) {
				shop = ShopSignData.from(s);
				if (shop != null) targetSign = s;
			}
			else if (be instanceof Inventory) {
				for (Direction dir : Direction.values()) {
					if (world.getBlockEntity(pos.offset(dir)) instanceof SignBlockEntity s2) {
						ShopSignData data = ShopSignData.from(s2);
						if (data != null) {
							targetSign = s2;
							shop = data;
							break;
						}
					}
				}
			}

			// 2. If it's not a shop, GET OUT immediately so blocks can be broken
			if (targetSign == null || shop == null) return ActionResult.PASS;

			// 3. Double Break Check
			boolean isAdmin = player.hasPermissionLevel(2);
			boolean isOwner = player.getEntityName().equalsIgnoreCase(shop.owner);

			if (player.isSneaking() && (isOwner || isAdmin)) {
				long now = System.currentTimeMillis();
				long last = adminBreakAttempts.getOrDefault(player.getUuid(), 0L);
				if (now - last < 5000) {
					adminBreakAttempts.remove(player.getUuid());
					return ActionResult.PASS; // Break the block!
				} else {
					adminBreakAttempts.put(player.getUuid(), now);
					player.sendMessage(Text.literal("§6Shop » §cPress again to break."), true);
					return ActionResult.FAIL;
				}
			}

			// 4. Sell Logic
			if (shop.sellPrice > 0) {
				if (shop.isAdminShop) {
					ShopProcessor.processSell((ServerPlayerEntity) player, shop, null);
				} else {
					Inventory chest = findAttachedInventory(world, targetSign.getPos());
					if (chest != null) {
						ShopProcessor.processSell((ServerPlayerEntity) player, shop, chest);
					}
				}
			}

			// 5. Ghost Fix (Force client to see the sign again)
			world.updateListeners(targetSign.getPos(), world.getBlockState(targetSign.getPos()), world.getBlockState(targetSign.getPos()), 3);
			if (player instanceof ServerPlayerEntity sp) {
				sp.networkHandler.sendPacket(BlockEntityUpdateS2CPacket.create(targetSign));
			}

			return ActionResult.FAIL; // Protect the block
		});
	}

	// Helper to find the inventory behind the sign
	private static Inventory findAttachedInventory(World world, BlockPos signPos) {
		BlockState state = world.getBlockState(signPos);
		Direction attachedDir = null;

		// 1. Logic-based search
		if (state.contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING)) {
			attachedDir = state.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING).getOpposite();
		} else {
			attachedDir = Direction.DOWN;
		}

		BlockPos targetPos = signPos.offset(attachedDir);
		BlockEntity be = world.getBlockEntity(targetPos);

		if (be instanceof Inventory inv) {
			System.out.println("DEBUG: Chest found at " + targetPos.toShortString() + " (Direction: " + attachedDir + ")");
			return inv;
		}

		// 2. Emergency Fallback (Radiant Search)
		System.out.println("DEBUG: No chest at " + targetPos.toShortString() + ". Searching surrounding blocks...");
		for (Direction dir : Direction.values()) {
			BlockPos fallbackPos = signPos.offset(dir);
			BlockEntity fallbackBe = world.getBlockEntity(fallbackPos);
			if (fallbackBe instanceof Inventory inv) {
				System.out.println("DEBUG: Fallback chest found at " + fallbackPos.toShortString() + " (Direction: " + dir + ")");
				return inv;
			}
		}

		System.out.println("DEBUG: FAILED - No inventory found adjacent to sign at " + signPos.toShortString());
		return null;
	}
}