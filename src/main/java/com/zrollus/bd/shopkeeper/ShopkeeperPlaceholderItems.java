package com.zrollus.bd.shopkeeper;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** Resolves renamed paper used as a player-shop placeholder into a real item type. */
public final class ShopkeeperPlaceholderItems {
    public static ItemStack replace(ItemStack stack, boolean adminShop) {
        if (adminShop || !stack.isOf(Items.PAPER)) return stack.copy();
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName == null || customName.getString().isBlank()) return stack.copy();

        String normalized = customName.getString().trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_').replace('-', '_');
        Identifier id = Identifier.tryParse(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (id == null || !Registries.ITEM.containsId(id)) return stack.copy();
        ItemStack replacement = new ItemStack(Registries.ITEM.get(id), stack.getCount());
        return replacement.isEmpty() ? stack.copy() : replacement;
    }

    private ShopkeeperPlaceholderItems() {}
}
