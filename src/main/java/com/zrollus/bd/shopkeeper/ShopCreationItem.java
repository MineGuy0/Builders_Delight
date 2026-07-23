package com.zrollus.bd.shopkeeper;

import com.zrollus.bd.Lib.ModConfigHelper;
import com.zrollus.bd.Lib.PermissionCompat;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShopCreationItem extends Item {
    private static final Shopkeeper.Type[] TYPES = {
            Shopkeeper.Type.SELLING, Shopkeeper.Type.BUYING, Shopkeeper.Type.TRADING, Shopkeeper.Type.BOOK
    };
    private static final String[] LEGACY_OBJECTS = {
            "minecraft:villager", "minecraft:wandering_trader", "minecraft:allay", "minecraft:piglin",
            "minecraft:fox", "minecraft:cat", "minecraft:wolf", "minecraft:cow", "minecraft:sheep",
            "minecraft:pig", "minecraft:chicken", "minecraft:rabbit", "minecraft:frog",
            "minecraft:axolotl", "minecraft:bee", "minecraft:blaze"
    };
    private static List<String> objectCache;

    public ShopCreationItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            NbtCompound data = data(stack);
            if (user.isSneaking()) cycleObject(data, world); else cycleType(data);
            save(stack, data);
            user.sendMessage(selection(data, world), true);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return ActionResult.PASS;
        ItemStack stack = context.getStack();
        ServerWorld world = player.getServerWorld();
        BlockPos clicked = context.getBlockPos();
        NbtCompound data = data(stack);

        if (ShopkeeperSystem.inventoryAt(world, clicked) != null) {
            data.putString("container_world", world.getRegistryKey().getValue().toString());
            data.putLong("container_pos", clicked.asLong());
            save(stack, data);
            player.sendMessage(Text.literal("Stock container selected. Now right-click the block where the shopkeeper should stand.").formatted(Formatting.GREEN), true);
            return ActionResult.SUCCESS;
        }
        if (!data.contains("container_pos")) {
            player.sendMessage(Text.literal("First right-click the chest, barrel, or other stock inventory.").formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        if (!worldId.equals(data.getString("container_world"))) {
            player.sendMessage(Text.literal("The stock container and shopkeeper must be in the same dimension.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        BlockPos container = BlockPos.fromLong(data.getLong("container_pos"));
        if (ShopkeeperSystem.inventoryAt(world, container) == null) {
            player.sendMessage(Text.literal("The selected stock container is no longer available.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        ShopkeeperManager manager = ShopkeeperManager.get(player.getServer());
        long owned = manager.all().stream().filter(s -> player.getUuid().equals(s.owner)).count();
        if (!PermissionCompat.check(player, "shopkeeper.admin", player.hasPermissionLevel(2)) && owned >= ModConfigHelper.get().maxShopkeepersPerPlayer) {
            player.sendMessage(Text.literal("You reached your shopkeeper limit.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        Shopkeeper shop = new Shopkeeper(UUID.randomUUID());
        shop.type = selectedType(data);
        shop.owner = player.getUuid();
        shop.ownerName = player.getName().getString();
        shop.name = shop.ownerName + "'s Shop";
        shop.exactItems = ModConfigHelper.get().defaultExactShopItems;
        shop.notifyOwner = ModConfigHelper.get().defaultShopTradeNotifications;
        shop.worldId = worldId;
        shop.containerWorldId = worldId;
        shop.containerPos = container.toImmutable();
        shop.objectType = selectedObject(data, world);
        if ("bd:player_shopkeeper".equals(shop.objectType))
            ShopkeeperSystem.usePlayerProfile(shop, player.getGameProfile());
        BlockPos spawnPos = clicked.up();
        if (!ShopkeeperSystem.hasPlacementRoom(world, spawnPos)) {
            player.sendMessage(Text.literal("The two blocks above the selected block must be clear.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        shop.objectPos = spawnPos.toImmutable();
        Identifier objectId = Identifier.tryParse(shop.objectType);
        if (objectId == null || ShopkeeperSystem.spawnObject(world, shop, objectId, Vec3d.ofBottomCenter(spawnPos)) == null) {
            player.sendMessage(Text.literal("That shopkeeper mob cannot be spawned here.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        manager.add(shop);
        if (!player.isCreative()) stack.decrement(1);
        player.sendMessage(Text.literal("Created " + shop.type.name().toLowerCase() + " shopkeeper.").formatted(Formatting.GREEN));
        ShopkeeperSystem.openEditor(player, shop);
        return ActionResult.SUCCESS;
    }

    public static ItemStack createStack(int amount) {
        ItemStack stack = new ItemStack(com.zrollus.bd.item.ModItems.SHOP_CREATION_ITEM, amount);
        NbtCompound data = new NbtCompound();
        data.putInt("type", 0);
        data.putString("object_id", "minecraft:villager");
        save(stack, data);
        return stack;
    }

    private static Shopkeeper.Type selectedType(NbtCompound data) {
        return TYPES[Math.floorMod(data.getInt("type"), TYPES.length)];
    }
    private static String selectedObject(NbtCompound data, World world) {
        List<String> objects = objectIds(world);
        String selected = data.getString("object_id");
        if (selected.isEmpty() && data.contains("object"))
            selected = LEGACY_OBJECTS[Math.floorMod(data.getInt("object"), LEGACY_OBJECTS.length)];
        return objects.contains(selected) ? selected : objects.getFirst();
    }
    private static void cycleType(NbtCompound data) { data.putInt("type", data.getInt("type") + 1); }
    private static void cycleObject(NbtCompound data, World world) {
        List<String> objects = objectIds(world);
        int current = objects.indexOf(selectedObject(data, world));
        data.putString("object_id", objects.get((current + 1) % objects.size()));
    }
    private static Text selection(NbtCompound data, World world) {
        return Text.literal("Shop: " + selectedType(data).name().toLowerCase() + " | Object: " + selectedObject(data, world)
                + " (" + objectIds(world).size() + " available)").formatted(Formatting.GOLD);
    }
    private static NbtCompound data(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }
    private static void save(ItemStack stack, NbtCompound data) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        String selected = data.getString("object_id");
        if (selected.isEmpty()) selected = "minecraft:villager";
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Shop Creation Item — "
                + selectedType(data).name().toLowerCase() + " / " + selected.substring(selected.indexOf(':') + 1)).formatted(Formatting.GOLD));
    }

    private static List<String> objectIds(World world) {
        if (objectCache != null) return objectCache;
        List<String> ids = new ArrayList<>();
        for (var type : Registries.ENTITY_TYPE) {
            try {
                Entity entity = type.create(world);
                if (entity instanceof LivingEntity) ids.add(Registries.ENTITY_TYPE.getId(type).toString());
                if (entity != null) entity.discard();
            } catch (RuntimeException ignored) {
                // Entity types which cannot be constructed directly are not safe shop objects.
            }
        }
        ids.sort(Comparator.comparingInt((String id) -> id.equals("minecraft:villager") ? 0
                : id.equals("bd:player_shopkeeper") ? 1 : 2).thenComparing(id -> id));
        if (ids.isEmpty()) ids.add("minecraft:villager");
        objectCache = List.copyOf(ids);
        return objectCache;
    }
}
