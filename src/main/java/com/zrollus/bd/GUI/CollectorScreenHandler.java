package com.zrollus.bd.GUI;

import com.zrollus.bd.Entity.CollectorBlockEntity;
import com.zrollus.bd.block.custom.CollectionRegion;
import com.zrollus.bd.block.custom.CollectorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class CollectorScreenHandler extends GenericContainerScreenHandler {
    private static final int MENU_SIZE = 54;
    private final CollectorBlockEntity collector;
    private final PropertyDelegate properties;

    public CollectorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null, new ArrayPropertyDelegate(CollectorBlockEntity.PROPERTY_COUNT));
    }

    public CollectorScreenHandler(int syncId, PlayerInventory playerInventory, CollectorBlockEntity collector) {
        this(syncId, playerInventory, collector, collector.getProperties());
    }

    private CollectorScreenHandler(int syncId, PlayerInventory playerInventory,
                                   CollectorBlockEntity collector, PropertyDelegate properties) {
        super(ModScreenHandlers.COLLECTOR, syncId, playerInventory, new SimpleInventory(MENU_SIZE), 6);
        this.collector = collector;
        this.properties = properties;
        addProperties(properties);
        if (collector != null) refresh();
    }

    @Override public boolean canUse(PlayerEntity player) {
        return collector == null || Inventory.canPlayerUse(collector, player);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < MENU_SIZE) {
            if (collector == null || !(player instanceof ServerPlayerEntity serverPlayer)) return;
            int delta = button == 1 ? -1 : 1;
            if (actionType == SlotActionType.QUICK_MOVE) delta *= 8;
            switch (slotIndex) {
                case 9 -> properties.set(4, properties.get(4) + delta);
                case 10 -> properties.set(5, properties.get(5) + delta);
                case 11 -> properties.set(6, properties.get(6) + delta);
                case 18 -> properties.set(7, properties.get(7) + delta);
                case 19 -> properties.set(8, properties.get(8) + delta);
                case 20 -> properties.set(9, properties.get(9) + delta);
                case 13 -> properties.set(10, properties.get(10) + 1);
                case 14 -> properties.set(11, properties.get(11) + 1);
                case 15 -> properties.set(12, properties.get(12) == 0 ? 1 : 0);
                case 16 -> properties.set(14, properties.get(14) + delta * 2);
                case 17 -> properties.set(15, properties.get(15) + delta * 8);
                case 22 -> properties.set(13, properties.get(13) == 0 ? 1 : 0);
                case 27, 28, 29, 30, 31, 32, 33, 34, 35 -> {
                    if (collector.getCollectorType() == CollectorBlock.Type.ITEM) {
                        collector.setFilter(slotIndex - 27, getCursorStack());
                    } else {
                        int amount = switch (slotIndex) {
                            case 27 -> 1;
                            case 28 -> 10;
                            case 29 -> 100;
                            case 30 -> 1000;
                            case 31 -> Integer.MAX_VALUE;
                            case 33 -> Math.min(10, serverPlayer.totalExperience);
                            case 34 -> Math.min(100, serverPlayer.totalExperience);
                            case 35 -> serverPlayer.totalExperience;
                            default -> 0;
                        };
                        if (slotIndex <= 31) collector.withdrawExperience(serverPlayer, amount);
                        else collector.depositExperience(serverPlayer, amount);
                    }
                }
                default -> { }
            }
            refresh();
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    private void refresh() {
        SimpleInventory menu = (SimpleInventory) getInventory();
        menu.clear();
        boolean item = collector.getCollectorType() == CollectorBlock.Type.ITEM;
        menu.setStack(4, named((item ? Items.HOPPER : Items.EXPERIENCE_BOTTLE).getDefaultStack(),
                item ? "Ranged Item Collector" : "Ranged XP Collector"));
        menu.setStack(9, value(Items.RED_STAINED_GLASS_PANE, "Range X", properties.get(4), "32 max"));
        menu.setStack(10, value(Items.GREEN_STAINED_GLASS_PANE, "Range Y", properties.get(5), "32 max"));
        menu.setStack(11, value(Items.BLUE_STAINED_GLASS_PANE, "Range Z", properties.get(6), "32 max"));
        menu.setStack(18, value(Items.RED_CONCRETE, "Offset X", properties.get(7), "-32 to 32"));
        menu.setStack(19, value(Items.GREEN_CONCRETE, "Offset Y", properties.get(8), "-32 to 32"));
        menu.setStack(20, value(Items.BLUE_CONCRETE, "Offset Z", properties.get(9), "-32 to 32"));
        menu.setStack(13, named(Items.STRUCTURE_VOID.getDefaultStack(),
                "Shape: " + title(CollectionRegion.Shape.values()[properties.get(10)].name())));
        menu.setStack(14, named(Items.REDSTONE_TORCH.getDefaultStack(),
                "Redstone: " + title(CollectorBlockEntity.RedstoneMode.values()[properties.get(11)].name())));
        menu.setStack(15, named((properties.get(12) == 0 ? Items.LIME_DYE : Items.RED_DYE).getDefaultStack(),
                properties.get(12) == 0 ? "Filter: Whitelist" : "Filter: Blacklist"));
        menu.setStack(16, value(Items.CLOCK, "Scan interval", properties.get(14), "ticks"));
        menu.setStack(17, value(Items.CHEST, "Item limit", properties.get(15), "per scan"));
        menu.setStack(22, named((properties.get(13) != 0 ? Items.ENDER_EYE : Items.ENDER_PEARL).getDefaultStack(),
                properties.get(13) != 0
                        ? "Boundary pinned: remains visible after closing this menu"
                        : "Boundary hidden: click to pin it in the world"));

        if (item) {
            for (int i = 0; i < CollectorBlockEntity.FILTER_SIZE; i++) {
                ItemStack filter = collector.getFilter(i);
                menu.setStack(27 + i, filter.isEmpty()
                        ? named(Items.GRAY_STAINED_GLASS_PANE.getDefaultStack(), "Filter slot " + (i + 1))
                        : filter.copy());
            }
            menu.setStack(49, named(Items.BOOK.getDefaultStack(),
                    "Left click +1, right click -1, shift click changes by 8. Pipes can extract from any side."));
        } else {
            menu.setStack(27, named(Items.EXPERIENCE_BOTTLE.getDefaultStack(), "Withdraw 1 XP"));
            menu.setStack(28, named(Items.EXPERIENCE_BOTTLE.getDefaultStack(), "Withdraw 10 XP"));
            menu.setStack(29, named(Items.EXPERIENCE_BOTTLE.getDefaultStack(), "Withdraw 100 XP"));
            menu.setStack(30, named(Items.EXPERIENCE_BOTTLE.getDefaultStack(), "Withdraw 1,000 XP"));
            menu.setStack(31, named(Items.DRAGON_BREATH.getDefaultStack(), "Withdraw all XP"));
            menu.setStack(33, named(Items.GLASS_BOTTLE.getDefaultStack(), "Deposit 10 XP"));
            menu.setStack(34, named(Items.GLASS_BOTTLE.getDefaultStack(), "Deposit 100 XP"));
            menu.setStack(35, named(Items.BUCKET.getDefaultStack(), "Deposit all XP"));
            menu.setStack(49, value(Items.SCULK, "Unbottled XP", collector.getStoredExperience(),
                    "• 7 XP per bottle • outputs to adjacent storage"));
        }
        sendContentUpdates();
    }

    public boolean previewEnabled() { return properties.get(13) != 0; }
    public CollectorBlock.Type getCollectorType() {
        return CollectorBlock.Type.values()[Math.floorMod(properties.get(3), CollectorBlock.Type.values().length)];
    }
    public int getRangeX() { return properties.get(4); }
    public int getRangeY() { return properties.get(5); }
    public int getRangeZ() { return properties.get(6); }
    public int getOffsetX() { return properties.get(7); }
    public int getOffsetY() { return properties.get(8); }
    public int getOffsetZ() { return properties.get(9); }
    public CollectorBlockEntity.RedstoneMode getRedstoneMode() {
        return CollectorBlockEntity.RedstoneMode.values()[
                Math.floorMod(properties.get(11), CollectorBlockEntity.RedstoneMode.values().length)];
    }
    public boolean isBlacklist() { return properties.get(12) != 0; }
    public int getInterval() { return properties.get(14); }
    public int getStackLimit() { return properties.get(15); }
    public int getStoredExperience() { return properties.get(16); }
    public CollectionRegion.Shape getShape() {
        return CollectionRegion.Shape.values()[Math.floorMod(properties.get(10), CollectionRegion.Shape.values().length)];
    }
    public BlockPos getCollectorPos() { return new BlockPos(properties.get(0), properties.get(1), properties.get(2)); }
    public Vec3d getCenter() {
        return Vec3d.ofCenter(getCollectorPos()).add(properties.get(7), properties.get(8), properties.get(9));
    }
    public Box getPreviewBox() {
        Vec3d center = getCenter();
        return new Box(center.x - properties.get(4), center.y - properties.get(5), center.z - properties.get(6),
                center.x + properties.get(4), center.y + properties.get(5), center.z + properties.get(6));
    }

    private static ItemStack value(net.minecraft.item.Item item, String label, int value, String suffix) {
        return named(item.getDefaultStack(), label + ": " + value + (suffix.isEmpty() ? "" : " " + suffix));
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.GOLD));
        return stack;
    }

    private static String title(String value) {
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
