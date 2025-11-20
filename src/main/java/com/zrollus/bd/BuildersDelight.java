package com.zrollus.bd;


import com.zrollus.bd.Entity.ModEntities;
import com.zrollus.bd.Lib.ModCommandManager;
import com.zrollus.bd.Sound.ModSounds;
import com.zrollus.bd.item.ModArmorEffects;
import net.fabricmc.api.ModInitializer;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.item.ModItems;
import com.zrollus.bd.item.ModItemGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildersDelight implements ModInitializer {
	public static final String MOD_ID = "bd";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModSounds.registerSounds();
		ModItemGroup.registerItemGroups();
		ModBlocks.RegisterModBlocks();
		ModEntities.init();
		ModItems.registerModItems();
		ModEnchantments.registerModEnchantments();
		ModArmorEffects.register();
		ModNetworking.init();
		ModCommandManager.register();
	}
}