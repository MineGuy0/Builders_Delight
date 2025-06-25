package com.zrollus.bd.item.Custom;

import net.minecraft.item.Item;

public class KeycardItem extends Item {
    private final int level;

    public KeycardItem(int level, Settings settings) {
        super(settings);
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

