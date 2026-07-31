package com.zrollus.bd;

import com.zrollus.bd.Entity.ModEntities;
import com.zrollus.bd.EventHandler.LockEventHandler;
import com.zrollus.bd.EventHandler.PlayerLifecycleHandler;
import com.zrollus.bd.EventHandler.ShopEventHandler;
import com.zrollus.bd.Lib.*;
import com.zrollus.bd.Sound.ModSounds;
import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.item.Custom.HammerItem;
import com.zrollus.bd.item.Custom.LesserDivinityHandler;
import com.zrollus.bd.item.ModArmorEffects;
import com.zrollus.bd.item.ModItemGroup;
import com.zrollus.bd.item.ModItems;
import com.zrollus.bd.world.ModWorldFeatures;
import com.zrollus.bd.world.TheBelowWorld;
import com.zrollus.bd.shopkeeper.ShopkeeperSystem;
import com.zrollus.bd.GUI.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildersDelight implements ModInitializer {
	public static final String MOD_ID = "bd";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 1. Core Registries
		ModConfigHelper.load();
		ModWorldFeatures.register();
		ModSounds.registerSounds();
		ModItemGroup.registerItemGroups();
		ModBlocks.RegisterModBlocks();
		ModEntities.init();
		ModItems.registerModItems();
		ModEnchantments.registerModEnchantments();
		ModScreenHandlers.register();
		ModArmorEffects.register();
		ModNetworking.init();
        ModCommandManager.register();
        com.zrollus.bd.Lib.ItemNameCommand.register();
        com.zrollus.bd.essentials.EssentialsSystem.register();
        com.zrollus.bd.essentials.EssentialsCommands.register();
		LesserDivinityHandler.register();
		FluidBreakerHandler.register();
		ShopkeeperSystem.register();
		TheBelowWorld.register();

		// 2. Event Handlers (The logic you had in registerEvents)
		PlayerLifecycleHandler.register(); // Handles Join, Disconnect, Death
		LockEventHandler.register();      // Handles /lock and Door protection
		ShopEventHandler.register();      // Handles Sign Shops (Buy/Sell/Protect)

		// 3. Async Startup Tasks
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			new Thread(() -> {
				try {
					Thread.sleep(5000);
					DynamicIdMapper mapper = DynamicIdMapper.getServerState(server);
					mapper.generateAllIds();
					LOGGER.info("BD Mod: Safe startup window reached.");
				} catch (InterruptedException ignored) {}
			}).start();
		});

		// Inside your Mod Initializer
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			BlockState state = world.getBlockState(pos);
			ItemStack stack = player.getStackInHand(hand);
			if (stack.getItem() instanceof HammerItem && player.isCreative()) {
				// Trigger the grid breaking manually for Creative
				HammerItem.executeHammerGrid(world, pos, player, stack);
			}
			return ActionResult.PASS;
		});

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!state.getFluidState().isEmpty()
					&& ModEnchantments.getLevel(player.getMainHandStack(), world.getRegistryManager(), ModEnchantments.FLUIDBREAKER) > 0) {
				HammerItem.playFluidBreakEffects((net.minecraft.server.world.ServerWorld) world, pos, state);
			}
		});
	}
}
