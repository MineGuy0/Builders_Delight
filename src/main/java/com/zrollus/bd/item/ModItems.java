package com.zrollus.bd.item;

import com.zrollus.bd.BuildersDelight;
import com.zrollus.bd.Sound.ModSounds;
import com.zrollus.bd.item.Custom.*;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

import static com.zrollus.bd.BuildersDelight.MOD_ID;

import static com.zrollus.bd.BuildersDelight.MOD_ID;

public class ModItems {
    public static final Item BANANA = registerItem("banana",
            new Item(new FabricItemSettings().food(new FoodComponent.Builder().hunger(10).saturationModifier(10f).build())));
    public static final Item CHERRY = registerItem("cherry",
            new Item(new FabricItemSettings().food(new FoodComponent.Builder().hunger(10).saturationModifier(10f).build())));
    public static final Item CLOAK = registerItem("cloak",
            new Item(new FabricItemSettings().fireproof()));
    public static final Item AETERNIUM_INGOT = registerItem("aeternium_ingot",
            new Item(new FabricItemSettings()));
    public static final Item POKEDOLLAR = registerItem("pokedollar",
            new Item(new FabricItemSettings()));
    public static final Item LIFE_WEAVER_SWORD = registerItem("life_weaver_sword",
            new LifeWeaverSword(ToolMaterials.NETHERITE,0,-2.4f, new Item.Settings().maxDamage(1561).fireproof(),0.2f));
    public static final Item HAMMER = registerItem("hammer",
            new HammerItem(ToolMaterials.NETHERITE, 5, -3.2f, new Item.Settings().maxDamage(2031).fireproof()));

    public static final Item STARDUST_SWORD = registerItem("stardust_sword",
            new SwordItem(ToolMaterials.NETHERITE, 9, 2.5f, new Item.Settings().maxDamage(3012).fireproof()));

    public static final Item LESSER_DIVINITY = registerItem("lesser_divinity",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));

    public static final Item PAXEL = registerItem("paxel",
            new PaxelItem(ModToolMaterial.RUBY, 8, 3f, new FabricItemSettings().fireproof()));
    public static final Item REAPER = registerItem("reaper",
            new ReaperItem(ModToolMaterial.RUBY, 8, 3f, new FabricItemSettings().fireproof()));
    public static final Item GALAXY_HELMET = registerItem("galaxy_helmet",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.HELMET, new FabricItemSettings().maxDamage(3254).fireproof()));
    public static final Item GALAXY_CHESTPLATE = registerItem("galaxy_chestplate",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.CHESTPLATE, new FabricItemSettings().maxDamage(4254).fireproof()));
    public static final Item GALAXY_LEGGINGS = registerItem("galaxy_leggings",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.LEGGINGS, new FabricItemSettings().maxDamage(4254).fireproof()));
    public static final Item GALAXY_BOOTS = registerItem("galaxy_boots",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.BOOTS, new FabricItemSettings().maxDamage(3254).fireproof()));

    public static final Item KEYCARD_1 = registerItem("keycard_1",
            new KeycardItem(1, new FabricItemSettings().maxCount(1)));
    public static final Item KEYCARD_2 = registerItem("keycard_2",
            new KeycardItem(2, new FabricItemSettings().maxCount(1)));
    public static final Item KEYCARD_3 = registerItem("keycard_3",
            new KeycardItem(3, new FabricItemSettings().maxCount(1)));
    public static final Item KEYCARD_4 = registerItem("keycard_4",
            new KeycardItem(4, new FabricItemSettings().maxCount(1)));
    public static final Item KEYCARD_5 = registerItem("keycard_5",
            new KeycardItem(5, new FabricItemSettings().maxCount(1)));
    public static final Item SUMMIT_DISC = registerItem("summit_disc",
            new MusicDiscItem(15, ModSounds.SUMMIT,new FabricItemSettings().maxCount(1), 668));
    public static final Item VALUE_DISC = registerItem("value_disc",
            new MusicDiscItem(15, ModSounds.VALUE, new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 185));
    public static final Item DOOMCROSSING_DISC = registerItem("doomcrossing_disc",
            new MusicDiscItem(15, ModSounds.DOOMCROSSING, new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 94));
    public static final Item HELLAGAIN_DISC = registerItem("hellagain_disc",
            new MusicDiscItem(15, ModSounds.HELLAGAIN, new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 208));
    public static final Item MILIHERO_DISC = registerItem("milihero_disc",
            new MusicDiscItem(15, ModSounds.HERO, new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 214));
    public static final Item TGD_DISC = registerItem("tgd_disc",
            new MusicDiscItem(15, ModSounds.TGD, new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 199));
    public static final Item SHUMMIC_DISC = registerItem("shummic_disc",
            new MusicDiscItem(15, ModSounds.SHUMMIC, new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 165));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(BuildersDelight.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BuildersDelight.LOGGER.debug("Registering mod items for " + BuildersDelight.MOD_ID);
    }
}
