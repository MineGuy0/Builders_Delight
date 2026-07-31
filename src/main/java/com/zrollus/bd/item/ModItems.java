package com.zrollus.bd.item;

import com.zrollus.bd.BuildersDelight;
import com.zrollus.bd.Sound.ModSounds;
import com.zrollus.bd.item.Custom.*;

import net.minecraft.item.*;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zrollus.bd.BuildersDelight.MOD_ID;

import static com.zrollus.bd.BuildersDelight.MOD_ID;

public class ModItems {
    public static final Item BANANA = registerItem("banana",
            new Item(new Item.Settings().food(new FoodComponent.Builder().nutrition(10).saturationModifier(10f).build())));
    public static final Item CHERRY = registerItem("cherry",
            new Item(new Item.Settings().food(new FoodComponent.Builder().nutrition(10).saturationModifier(10f).build())));
    public static final Item CLOAK = registerItem("cloak",
            new CloakItem(new Item.Settings().fireproof().attributeModifiers(CloakItem.createAttributeModifiers())));
    public static final Item AETERNIUM_INGOT = registerItem("aeternium_ingot",
            new Item(new Item.Settings()));
    public static final Item POKEDOLLAR = registerItem("pokedollar",
            new Item(new Item.Settings()));
    public static final Item SHOP_CREATION_ITEM = registerItem("shop_creation_item",
            new com.zrollus.bd.shopkeeper.ShopCreationItem(new Item.Settings().maxCount(16)));
    public static final Item RIFT_SHARD = registerItem("rift_shard",
            new RiftShardItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()));
    public static final Item LIFE_WEAVER_SWORD = registerItem("life_weaver_sword",
            new LifeWeaverSword(ToolMaterials.NETHERITE,0,-2.4f, new Item.Settings().maxDamage(1561).fireproof(),0.2f));
    public static final Item HAMMER = registerItem("hammer",
            new HammerItem(ToolMaterials.NETHERITE, 5, -3.2f, new Item.Settings().maxDamage(2031).fireproof()));
    public static final Item BATTLE_AXE = registerItem("battle_axe",
            new BattleAxeItem(ToolMaterials.NETHERITE, 11.0F, -3.1F,
                    new Item.Settings().maxDamage(2440).fireproof().rarity(Rarity.EPIC)));

    public static final Item STARDUST_SWORD = registerItem("stardust_sword",
            new SwordItem(ToolMaterials.NETHERITE, new Item.Settings().maxDamage(3012).fireproof()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 9, 2.5f))));

    public static final Item LESSER_DIVINITY = registerItem("lesser_divinity",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

    public static final Item PAXEL = registerItem("paxel",
            new PaxelItem(ModToolMaterial.RUBY, 8, 3f, new Item.Settings().fireproof()));
    public static final Item REAPER = registerItem("reaper",
            new ReaperItem(ModToolMaterial.RUBY, 8, 3f, new Item.Settings().fireproof()));
    public static final Item GALAXY_HELMET = registerItem("galaxy_helmet",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.HELMET, new Item.Settings().maxDamage(3254).fireproof()));
    public static final Item GALAXY_CHESTPLATE = registerItem("galaxy_chestplate",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxDamage(4254).fireproof()));
    public static final Item GALAXY_LEGGINGS = registerItem("galaxy_leggings",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.LEGGINGS, new Item.Settings().maxDamage(4254).fireproof()));
    public static final Item GALAXY_BOOTS = registerItem("galaxy_boots",
            new ArmorItem(ModArmorMaterials.STARDUST, ArmorItem.Type.BOOTS, new Item.Settings().maxDamage(3254).fireproof()));

    public static final Item KEYCARD_1 = registerItem("keycard_1",
            new KeycardItem(1, new Item.Settings().maxCount(1)));
    public static final Item KEYCARD_2 = registerItem("keycard_2",
            new KeycardItem(2, new Item.Settings().maxCount(1)));
    public static final Item KEYCARD_3 = registerItem("keycard_3",
            new KeycardItem(3, new Item.Settings().maxCount(1)));
    public static final Item KEYCARD_4 = registerItem("keycard_4",
            new KeycardItem(4, new Item.Settings().maxCount(1)));
    public static final Item KEYCARD_5 = registerItem("keycard_5",
            new KeycardItem(5, new Item.Settings().maxCount(1)));
    public static final Item SUMMIT_DISC = registerItem("summit_disc",
            new Item(new Item.Settings().maxCount(1)));
    public static final Item VALUE_DISC = registerItem("value_disc",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item DOOMCROSSING_DISC = registerItem("doomcrossing_disc",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item HELLAGAIN_DISC = registerItem("hellagain_disc",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item MILIHERO_DISC = registerItem("milihero_disc",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item TGD_DISC = registerItem("tgd_disc",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item SHUMMIC_DISC = registerItem("shummic_disc",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

    /**
     * Imported display-only items. These deliberately use the base Item class:
     * no tool behavior, attacks, abilities, food values, or other mechanics.
     */
    public static final Map<String, Item> IMPORTED_DISPLAY_ITEMS = registerImportedDisplayItems();

    private static Map<String, Item> registerImportedDisplayItems() {
        Map<String, Item> items = new LinkedHashMap<>();
        for (String id : List.of(
                "stupid_star",
                "desert_eagle",
                "chaos_fist",
                "party_hat",
                "tokito_nichirin",
                "master_emerald",
                "keksis_sword",
                "ritual",
                "amy_axe",
                "starless_longsword",
                "orb_shard",
                "midus_staff",
                "blue_shark",
                "willbreaker",
                "orb_of_dominance",
                "prime",
                "lunchly_bundle",
                "lunchly_bundle_filled",
                "mini_pizza",
                "feastables",
                "excalibur",
                "void_blade",
                "praxis_blade",
                "voidwalker",
                "sylvanfang",
                "solstice",
                "memer_hammer",
                "scar_scythe",
                "stormlander",
                "sculk_scythe",
                "lostvayne",
                "galaxy_sword",
                "creation_scythe",
                "revenants_gravescepter",
                "tiny_creation_scythe"
        )) {
            items.put(id, registerItem(id, new Item(new Item.Settings())));
        }
        return Collections.unmodifiableMap(items);
    }


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(BuildersDelight.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BuildersDelight.LOGGER.debug("Registering mod items for " + BuildersDelight.MOD_ID);
    }
}
