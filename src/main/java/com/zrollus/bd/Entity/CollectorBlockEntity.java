package com.zrollus.bd.Entity;

import com.zrollus.bd.GUI.CollectorScreenHandler;
import com.zrollus.bd.block.custom.CollectionRegion;
import com.zrollus.bd.block.custom.CollectorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CollectorBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public enum RedstoneMode {
        HIGH, LOW, PULSE, ALWAYS;
        public RedstoneMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public static final int BUFFER_SIZE = 9;
    public static final int FILTER_SIZE = 9;
    public static final int PROPERTY_COUNT = 17;
    private static final int EXPERIENCE_PER_BOTTLE = 7;

    private final DefaultedList<ItemStack> buffer = DefaultedList.ofSize(BUFFER_SIZE, ItemStack.EMPTY);
    private final DefaultedList<ItemStack> filters = DefaultedList.ofSize(FILTER_SIZE, ItemStack.EMPTY);
    private final CollectionRegion region = new CollectionRegion();
    private RedstoneMode redstoneMode = RedstoneMode.HIGH;
    private boolean blacklist;
    private boolean preview = true;
    private boolean pulsePending;
    private int interval = 10;
    private int stackLimit = 64;
    private int storedExperience;
    private int ticks;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> pos.getX();
                case 1 -> pos.getY();
                case 2 -> pos.getZ();
                case 3 -> getCollectorType().ordinal();
                case 4 -> region.rangeX();
                case 5 -> region.rangeY();
                case 6 -> region.rangeZ();
                case 7 -> region.offsetX();
                case 8 -> region.offsetY();
                case 9 -> region.offsetZ();
                case 10 -> region.shape().ordinal();
                case 11 -> redstoneMode.ordinal();
                case 12 -> blacklist ? 1 : 0;
                case 13 -> preview ? 1 : 0;
                case 14 -> interval;
                case 15 -> stackLimit;
                case 16 -> storedExperience;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            switch (index) {
                case 4 -> region.setRangeX(value);
                case 5 -> region.setRangeY(value);
                case 6 -> region.setRangeZ(value);
                case 7 -> region.setOffsetX(value);
                case 8 -> region.setOffsetY(value);
                case 9 -> region.setOffsetZ(value);
                case 10 -> region.setShape(value);
                case 11 -> redstoneMode = RedstoneMode.values()[Math.floorMod(value, RedstoneMode.values().length)];
                case 12 -> blacklist = value != 0;
                case 13 -> preview = value != 0;
                case 14 -> interval = Math.max(2, Math.min(40, value));
                case 15 -> stackLimit = Math.max(1, Math.min(64, value));
                case 16 -> storedExperience = Math.max(0, value);
                default -> { }
            }
            changed();
        }

        @Override public int size() { return PROPERTY_COUNT; }
    };

    public CollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModEntities.COLLECTOR, pos, state);
    }

    public CollectorBlock.Type getCollectorType() {
        return getCachedState().getBlock() instanceof CollectorBlock block
                ? block.getCollectorType() : CollectorBlock.Type.ITEM;
    }

    public CollectionRegion getRegion() { return region; }
    public PropertyDelegate getProperties() { return properties; }
    public ItemStack getFilter(int slot) { return filters.get(slot); }
    public RedstoneMode getRedstoneMode() { return redstoneMode; }
    public boolean isBlacklist() { return blacklist; }
    public boolean isPreviewEnabled() { return preview; }
    public int getInterval() { return interval; }
    public int getStackLimit() { return stackLimit; }
    public int getStoredExperience() { return storedExperience; }

    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= filters.size()) return;
        filters.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        changed();
    }

    public void onPowerChanged(boolean powered) {
        if (powered && redstoneMode == RedstoneMode.PULSE) pulsePending = true;
        changed();
    }

    public void withdrawExperience(ServerPlayerEntity player, int amount) {
        int moved = Math.min(storedExperience, Math.max(0, amount));
        if (moved <= 0) return;
        storedExperience -= moved;
        player.addExperience(moved);
        changed();
    }

    public void depositExperience(ServerPlayerEntity player, int amount) {
        int moved = Math.min(Math.max(0, amount), player.totalExperience);
        if (moved <= 0 || storedExperience > Integer.MAX_VALUE - moved) return;
        player.addExperience(-moved);
        storedExperience += moved;
        changed();
    }

    public static void tick(World world, BlockPos pos, BlockState state, CollectorBlockEntity collector) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        collector.ticks++;
        boolean powered = state.contains(CollectorBlock.POWERED) && state.get(CollectorBlock.POWERED);
        boolean active = switch (collector.redstoneMode) {
            case HIGH -> powered;
            case LOW -> !powered;
            case PULSE -> collector.pulsePending;
            case ALWAYS -> true;
        };
        if (!active || collector.ticks % collector.interval != 0) return;
        if (collector.redstoneMode == RedstoneMode.PULSE) collector.pulsePending = false;
        if (!collector.region.isLoaded(serverWorld, pos)) return;

        if (collector.getCollectorType() == CollectorBlock.Type.ITEM) collector.collectItems(serverWorld);
        else collector.collectExperience(serverWorld);
    }

    private void collectItems(ServerWorld world) {
        pushBuffer(world);
        int remaining = stackLimit;
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, region.bounds(pos),
                entity -> entity.isAlive() && !entity.getStack().isEmpty()
                        && region.contains(pos, entity.getPos()) && accepts(entity.getStack()));
        for (ItemEntity entity : items) {
            if (remaining <= 0) break;
            ItemStack stack = entity.getStack();
            int moved = insert(this, stack, Math.min(remaining, stack.getCount()), null);
            if (moved <= 0) continue;
            stack.decrement(moved);
            remaining -= moved;
            if (stack.isEmpty()) entity.discard();
            else entity.setStack(stack);
        }
        if (remaining != stackLimit) {
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.3F, 1.4F);
            pushBuffer(world);
            changed();
        }
    }

    private void collectExperience(ServerWorld world) {
        // Make room first so bottles waiting in the internal buffer can enter an
        // attached chest, barrel, pipe, or other inventory.
        pushBuffer(world);

        int collected = 0;
        List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, region.bounds(pos),
                entity -> entity.isAlive() && region.contains(pos, entity.getPos()));
        for (ExperienceOrbEntity orb : orbs) {
            int value = orb.getExperienceAmount();
            if (value <= 0 || storedExperience > Integer.MAX_VALUE - value) continue;
            storedExperience += value;
            collected += value;
            orb.discard();
        }

        int availableBottles = Math.min(stackLimit, storedExperience / EXPERIENCE_PER_BOTTLE);
        int bottled = 0;
        if (availableBottles > 0) {
            bottled = insert(this, new ItemStack(Items.EXPERIENCE_BOTTLE), availableBottles, null);
            storedExperience -= bottled * EXPERIENCE_PER_BOTTLE;
        }

        if (collected > 0 || bottled > 0) {
            world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.BLOCKS, 0.35F, 1.2F);
            if (bottled > 0) {
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL,
                        SoundCategory.BLOCKS, 0.45F, 1.35F);
                pushBuffer(world);
            }
            changed();
        }
    }

    private boolean accepts(ItemStack stack) {
        boolean any = false;
        boolean matched = false;
        for (ItemStack filter : filters) {
            if (filter.isEmpty()) continue;
            any = true;
            if (ItemStack.areItemsAndComponentsEqual(filter, stack)) {
                matched = true;
                break;
            }
        }
        if (!any) return true;
        return blacklist != matched;
    }

    private void pushBuffer(ServerWorld world) {
        for (Direction direction : Direction.values()) {
            Inventory destination = HopperBlockEntity.getInventoryAt(world, pos.offset(direction));
            if (destination == null || destination == this) continue;
            for (int slot = 0; slot < size(); slot++) {
                ItemStack stack = getStack(slot);
                if (stack.isEmpty()) continue;
                int moved = insert(destination, stack, stack.getCount(), direction.getOpposite());
                if (moved > 0) stack.decrement(moved);
            }
        }
    }

    private static int insert(Inventory inventory, ItemStack template, int amount, @Nullable Direction side) {
        int remaining = amount;
        for (int slot : slots(inventory, side)) {
            ItemStack present = inventory.getStack(slot);
            if (!present.isEmpty() && ItemStack.areItemsAndComponentsEqual(present, template)
                    && canInsert(inventory, slot, template, side)) {
                int max = Math.min(present.getMaxCount(), inventory.getMaxCount(present));
                int moved = Math.min(remaining, Math.max(0, max - present.getCount()));
                if (moved > 0) {
                    present.increment(moved);
                    remaining -= moved;
                }
                if (remaining == 0) break;
            }
        }
        for (int slot : slots(inventory, side)) {
            if (remaining == 0) break;
            if (inventory.getStack(slot).isEmpty() && canInsert(inventory, slot, template, side)) {
                int moved = Math.min(remaining, Math.min(template.getMaxCount(), inventory.getMaxCount(template)));
                inventory.setStack(slot, template.copyWithCount(moved));
                remaining -= moved;
            }
        }
        if (remaining != amount) inventory.markDirty();
        return amount - remaining;
    }

    private static int[] slots(Inventory inventory, @Nullable Direction side) {
        if (side != null && inventory instanceof SidedInventory sided) return sided.getAvailableSlots(side);
        int[] result = new int[inventory.size()];
        for (int i = 0; i < result.length; i++) result[i] = i;
        return result;
    }

    private static boolean canInsert(Inventory inventory, int slot, ItemStack stack, @Nullable Direction side) {
        return inventory.isValid(slot, stack)
                && (side == null || !(inventory instanceof SidedInventory sided) || sided.canInsert(slot, stack, side));
    }

    public void changed() {
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, buffer, registries);
        NbtList filterList = new NbtList();
        for (int i = 0; i < filters.size(); i++) {
            if (filters.get(i).isEmpty()) continue;
            NbtCompound entry = new NbtCompound();
            entry.putInt("slot", i);
            entry.put("stack", filters.get(i).encode(registries));
            filterList.add(entry);
        }
        nbt.put("filters", filterList);
        region.writeNbt(nbt);
        nbt.putInt("redstone_mode", redstoneMode.ordinal());
        nbt.putBoolean("blacklist", blacklist);
        nbt.putBoolean("preview", preview);
        nbt.putInt("interval", interval);
        nbt.putInt("stack_limit", stackLimit);
        nbt.putInt("stored_experience", storedExperience);
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, buffer, registries);
        for (int i = 0; i < filters.size(); i++) filters.set(i, ItemStack.EMPTY);
        NbtList filterList = nbt.getList("filters", 10);
        for (int i = 0; i < filterList.size(); i++) {
            NbtCompound entry = filterList.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot >= 0 && slot < filters.size()) {
                filters.set(slot, ItemStack.fromNbtOrEmpty(registries, entry.getCompound("stack")));
            }
        }
        region.readNbt(nbt);
        redstoneMode = RedstoneMode.values()[Math.floorMod(nbt.getInt("redstone_mode"), RedstoneMode.values().length)];
        blacklist = nbt.getBoolean("blacklist");
        preview = !nbt.contains("preview") || nbt.getBoolean("preview");
        interval = Math.max(2, Math.min(40, nbt.getInt("interval")));
        if (interval == 2 && !nbt.contains("interval")) interval = 10;
        stackLimit = Math.max(1, Math.min(64, nbt.getInt("stack_limit")));
        if (stackLimit == 1 && !nbt.contains("stack_limit")) stackLimit = 64;
        storedExperience = Math.max(0, nbt.getInt("stored_experience"));
    }

    @Override public Text getDisplayName() {
        return Text.translatable(getCollectorType() == CollectorBlock.Type.ITEM
                ? "container.bd.item_collector" : "container.bd.experience_collector");
    }

    @Nullable
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CollectorScreenHandler(syncId, playerInventory, this);
    }

    @Override public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override public int size() { return buffer.size(); }
    @Override public boolean isEmpty() { return buffer.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return buffer.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(buffer, slot, amount);
        if (!result.isEmpty()) changed();
        return result;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(buffer, slot);
        if (!result.isEmpty()) changed();
        return result;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        buffer.set(slot, stack);
        stack.capCount(getMaxCount(stack));
        changed();
    }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public void clear() { buffer.clear(); changed(); }
}
