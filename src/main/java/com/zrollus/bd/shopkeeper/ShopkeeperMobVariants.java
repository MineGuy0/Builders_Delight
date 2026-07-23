package com.zrollus.bd.shopkeeper;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.util.DyeColor;

import java.util.List;

/** Cycles vanilla 1.21.1 mob appearance data while preserving the rest of the entity NBT. */
public final class ShopkeeperMobVariants {
    private static final List<String> CAT = List.of("minecraft:tabby", "minecraft:black", "minecraft:red",
            "minecraft:siamese", "minecraft:british_shorthair", "minecraft:calico", "minecraft:persian",
            "minecraft:ragdoll", "minecraft:white", "minecraft:jellie", "minecraft:all_black");
    private static final List<String> WOLF = List.of("minecraft:pale", "minecraft:spotted", "minecraft:snowy",
            "minecraft:black", "minecraft:ashen", "minecraft:rusty", "minecraft:woods", "minecraft:chestnut",
            "minecraft:striped");
    private static final List<String> FROG = List.of("minecraft:temperate", "minecraft:warm", "minecraft:cold");
    private static final List<String> FOX = List.of("red", "snow");
    private static final List<String> MOOSHROOM = List.of("red", "brown");
    private static final List<String> PANDA = List.of("normal", "lazy", "worried", "playful", "brown", "weak", "aggressive");
    private static final List<String> VILLAGER_TYPES = List.of("minecraft:plains", "minecraft:desert", "minecraft:jungle",
            "minecraft:savanna", "minecraft:snow", "minecraft:swamp", "minecraft:taiga");
    private static final List<String> VILLAGER_PROFESSIONS = List.of("minecraft:none", "minecraft:armorer", "minecraft:butcher",
            "minecraft:cartographer", "minecraft:cleric", "minecraft:farmer", "minecraft:fisherman", "minecraft:fletcher",
            "minecraft:leatherworker", "minecraft:librarian", "minecraft:mason", "minecraft:nitwit", "minecraft:shepherd",
            "minecraft:toolsmith", "minecraft:weaponsmith");
    private static final int[] RABBIT = {0, 1, 2, 3, 4, 5, 99};

    public static String cycle(Entity entity, boolean secondary, boolean backwards) {
        return cycleChannel(entity, secondary ? 1 : 0, backwards);
    }

    public static String cycleChannel(Entity entity, int channel, boolean backwards) {
        NbtCompound nbt = new NbtCompound();
        entity.writeNbt(nbt);
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        boolean changed;

        if (channel == 0) {
            changed = switch (id) {
                case "minecraft:axolotl" -> cycleInt(nbt, "Variant", 0, 4, backwards);
                case "minecraft:cat" -> cycleString(nbt, "variant", CAT, backwards);
                case "minecraft:wolf" -> cycleString(nbt, "variant", WOLF, backwards);
                case "minecraft:frog" -> cycleString(nbt, "variant", FROG, backwards);
                case "minecraft:fox" -> cycleString(nbt, "Type", FOX, backwards);
                case "minecraft:mooshroom" -> cycleString(nbt, "Type", MOOSHROOM, backwards);
                case "minecraft:rabbit" -> cycleRabbit(nbt, backwards);
                case "minecraft:parrot" -> cycleInt(nbt, "Variant", 0, 4, backwards);
                case "minecraft:sheep" -> cycleInt(nbt, "Color", 0, 15, backwards);
                case "minecraft:horse" -> cycleHorse(nbt, backwards);
                case "minecraft:llama", "minecraft:trader_llama" -> cycleInt(nbt, "Variant", 0, 3, backwards);
                case "minecraft:tropical_fish" -> cycleTropical(nbt, 0, backwards);
                case "minecraft:panda" -> cyclePanda(nbt, backwards);
                case "minecraft:goat" -> toggle(nbt, "IsScreamingGoat");
                case "minecraft:creeper" -> toggle(nbt, "powered");
                case "minecraft:snow_golem" -> toggle(nbt, "Pumpkin");
                case "minecraft:shulker" -> cycleInt(nbt, "Color", -1, 15, backwards);
                case "minecraft:slime", "minecraft:magma_cube" -> cycleInt(nbt, "Size", 0, 3, backwards);
                case "minecraft:phantom" -> cycleInt(nbt, "Size", 0, 5, backwards);
                case "minecraft:armor_stand" -> toggle(nbt, "NoBasePlate");
                case "minecraft:pufferfish" -> cycleInt(nbt, "PuffState", 0, 2, backwards);
                case "minecraft:bogged" -> toggle(nbt, "Sheared");
                case "minecraft:zombie_villager" -> cycleVillagerData(nbt, "profession", VILLAGER_PROFESSIONS, backwards);
                default -> false;
            };
        } else if (channel == 1) {
            changed = switch (id) {
                case "minecraft:cat", "minecraft:wolf" -> cycleInt(nbt, "CollarColor", 0, 15, backwards);
                case "minecraft:fox" -> toggle(nbt, "Sleeping");
                case "minecraft:sheep" -> toggle(nbt, "Sheared");
                case "minecraft:goat" -> toggle(nbt, "HasLeftHorn");
                case "minecraft:armor_stand" -> toggle(nbt, "ShowArms");
                case "minecraft:donkey", "minecraft:mule", "minecraft:llama", "minecraft:trader_llama" -> toggle(nbt, "ChestedHorse");
                case "minecraft:pig", "minecraft:strider" -> toggle(nbt, "Saddle");
                case "minecraft:tropical_fish" -> cycleTropical(nbt, 1, backwards);
                case "minecraft:zombie_villager" -> cycleVillagerData(nbt, "type", VILLAGER_TYPES, backwards);
                default -> false;
            };
        } else if (channel == 2) {
            changed = switch (id) {
                case "minecraft:wolf" -> toggle(nbt, "Angry");
                case "minecraft:cat" -> toggle(nbt, "Sitting");
                case "minecraft:fox" -> toggle(nbt, "Crouching");
                case "minecraft:goat" -> toggle(nbt, "HasRightHorn");
                case "minecraft:armor_stand" -> toggle(nbt, "Small");
                case "minecraft:tropical_fish" -> cycleTropical(nbt, 2, backwards);
                default -> false;
            };
        } else {
            changed = cycleAge(nbt);
        }

        if (!changed) return "No additional variant is exposed for that control on this mob.";
        entity.readNbt(nbt);
        return description(entity);
    }

    public static String description(Entity entity) {
        NbtCompound nbt = new NbtCompound();
        entity.writeNbt(nbt);
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        String primary = firstPresent(nbt, "variant", "Variant", "Type", "RabbitType", "Color", "Size",
                "MainGene", "IsScreamingGoat", "powered", "Pumpkin");
        String age = nbt.contains("Age") ? (nbt.getInt("Age") < 0 ? "baby" : "adult")
                : nbt.contains("IsBaby") ? (nbt.getBoolean("IsBaby") ? "baby" : "adult") : "";
        return "Variant updated: " + id + (primary.isEmpty() ? "" : " | " + primary)
                + (age.isEmpty() ? "" : " | " + age);
    }

    private static boolean cycleAge(NbtCompound nbt) {
        if (nbt.contains("Age")) {
            nbt.putInt("Age", nbt.getInt("Age") < 0 ? 0 : -24000);
            return true;
        }
        if (nbt.contains("IsBaby")) return toggle(nbt, "IsBaby");
        return false;
    }

    private static boolean cycleRabbit(NbtCompound nbt, boolean backwards) {
        int current = nbt.getInt("RabbitType");
        int index = 0;
        for (int i = 0; i < RABBIT.length; i++) if (RABBIT[i] == current) index = i;
        index = Math.floorMod(index + (backwards ? -1 : 1), RABBIT.length);
        nbt.putInt("RabbitType", RABBIT[index]);
        return true;
    }

    private static boolean cycleHorse(NbtCompound nbt, boolean backwards) {
        int current = nbt.getInt("Variant");
        int color = Math.min(current & 255, 6);
        int marking = Math.min((current >> 8) & 255, 4);
        int index = marking * 7 + color;
        index = Math.floorMod(index + (backwards ? -1 : 1), 35);
        nbt.putInt("Variant", (index % 7) | ((index / 7) << 8));
        return true;
    }

    private static boolean cycleTropical(NbtCompound nbt, int channel, boolean backwards) {
        TropicalFishEntity.Variant current = new TropicalFishEntity.Variant(nbt.getInt("Variant"));
        var varieties = TropicalFishEntity.Variety.values();
        TropicalFishEntity.Variety variety = current.variety();
        DyeColor base = current.baseColor();
        DyeColor pattern = current.patternColor();
        int direction = backwards ? -1 : 1;
        if (channel == 0) variety = varieties[Math.floorMod(variety.ordinal() + direction, varieties.length)];
        else if (channel == 1) base = DyeColor.byId(base.getId() + direction);
        else pattern = DyeColor.byId(pattern.getId() + direction);
        nbt.putInt("Variant", new TropicalFishEntity.Variant(variety, base, pattern).getId());
        return true;
    }

    private static boolean cyclePanda(NbtCompound nbt, boolean backwards) {
        String current = nbt.getString("MainGene");
        int index = PANDA.indexOf(current);
        index = Math.floorMod((index < 0 ? 0 : index) + (backwards ? -1 : 1), PANDA.size());
        nbt.putString("MainGene", PANDA.get(index));
        nbt.putString("HiddenGene", PANDA.get(index));
        return true;
    }

    private static boolean cycleVillagerData(NbtCompound nbt, String key, List<String> values, boolean backwards) {
        NbtCompound data = nbt.getCompound("VillagerData");
        cycleString(data, key, values, backwards);
        nbt.put("VillagerData", data);
        return true;
    }

    private static boolean cycleInt(NbtCompound nbt, String key, int min, int max, boolean backwards) {
        int value = nbt.contains(key) ? nbt.getInt(key) : min;
        int count = max - min + 1;
        nbt.putInt(key, min + Math.floorMod(value - min + (backwards ? -1 : 1), count));
        return true;
    }

    private static boolean cycleString(NbtCompound nbt, String key, List<String> values, boolean backwards) {
        int index = values.indexOf(nbt.getString(key));
        index = Math.floorMod((index < 0 ? 0 : index) + (backwards ? -1 : 1), values.size());
        nbt.putString(key, values.get(index));
        return true;
    }

    private static boolean toggle(NbtCompound nbt, String key) {
        nbt.putBoolean(key, !nbt.getBoolean(key));
        return true;
    }

    private static String firstPresent(NbtCompound nbt, String... keys) {
        for (String key : keys) if (nbt.contains(key)) return key + "=" + nbt.get(key);
        return "";
    }

    private ShopkeeperMobVariants() {}
}
