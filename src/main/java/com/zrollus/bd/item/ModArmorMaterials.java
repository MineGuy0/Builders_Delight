package com.zrollus.bd.item;

import com.zrollus.bd.BuildersDelight;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModArmorMaterials {
    public static final RegistryEntry<ArmorMaterial> STARDUST = Registry.registerReference(
            Registries.ARMOR_MATERIAL,
            Identifier.of(BuildersDelight.MOD_ID, "stardust"),
            new ArmorMaterial(defense(), 19, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.ofItems(ModItems.AETERNIUM_INGOT),
                    List.of(new ArmorMaterial.Layer(Identifier.of(BuildersDelight.MOD_ID, "stardust"))),
                    5.0f, 1.0f)
    );

    private static Map<ArmorItem.Type, Integer> defense() {
        Map<ArmorItem.Type, Integer> values = new EnumMap<>(ArmorItem.Type.class);
        values.put(ArmorItem.Type.BOOTS, 6);
        values.put(ArmorItem.Type.LEGGINGS, 12);
        values.put(ArmorItem.Type.CHESTPLATE, 16);
        values.put(ArmorItem.Type.HELMET, 6);
        values.put(ArmorItem.Type.BODY, 16);
        return values;
    }

    private ModArmorMaterials() {
    }
}
