package com.zrollus.bd.item;

import com.zrollus.bd.BuildersDelight;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {
    STARDUST("stardust", 50, new int[] {6, 12, 16, 6}, 19,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 5.0f, 1.0f, ()  -> Ingredient.ofItems(ModItems.AETERNIUM_INGOT)  )


    ;

    private final String name;
    private final int duabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantablility;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistence;
    private final Supplier<Ingredient> repairIngredient;
    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

    ModArmorMaterials(String name, int duabilityMultiplier, int[] protectionAmounts, int enchantablility, SoundEvent equipSound,
                      float toughness, float knockbackResistence, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.duabilityMultiplier = duabilityMultiplier;
        this.protectionAmounts = protectionAmounts;
        this.enchantablility = enchantablility;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistence = knockbackResistence;
        this.repairIngredient = repairIngredient;
    }


    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()];
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return protectionAmounts[type.ordinal()];
    }

    @Override
    public int getEnchantability() {
        return this.enchantablility;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return BuildersDelight.MOD_ID + ":" + this.name().toLowerCase();
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistence;
    }
}
