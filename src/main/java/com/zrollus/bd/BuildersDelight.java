package com.zrollus.bd;

import com.zrollus.bd.Entity.ModEntities;
import com.zrollus.bd.EventHandler.LockEventHandler;
import com.zrollus.bd.EventHandler.PlayerLifecycleHandler;
import com.zrollus.bd.EventHandler.ShopEventHandler;
import com.zrollus.bd.Lib.*;
import com.zrollus.bd.Sound.ModSounds;
import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.item.Custom.LesserDivinityHandler;
import com.zrollus.bd.item.ModArmorEffects;
import com.zrollus.bd.item.ModItemGroup;
import com.zrollus.bd.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildersDelight implements ModInitializer {
	public static final String MOD_ID = "bd";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 1. Core Registries
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
	}
}