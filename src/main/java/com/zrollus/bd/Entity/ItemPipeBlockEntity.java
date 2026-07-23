package com.zrollus.bd.Entity;

import com.zrollus.bd.GUI.ItemPipeScreenHandler;
import com.zrollus.bd.block.custom.ItemPipeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** A colored pipe node with a persisted, visible two-tick item transport stage. */
public final class ItemPipeBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private static final int MAX_NETWORK_PIPES = 1024;
    public static final int TICKS_PER_PIPE = 2;
    private final DefaultedList<ItemStack> whitelist = DefaultedList.ofSize(9, ItemStack.EMPTY);

    private ItemStack movingStack = ItemStack.EMPTY;
    private List<BlockPos> route = List.of();
    private int routeIndex;
    private BlockPos destinationStorage;
    private Direction travelFrom = Direction.WEST;
    private Direction travelTo = Direction.EAST;
    private int travelTicks;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModEntities.ITEM_PIPE, pos, state);
    }

    public ItemPipeBlock.Role getRole() {
        return getCachedState().getBlock() instanceof ItemPipeBlock pipe
                ? pipe.getRole() : ItemPipeBlock.Role.CONNECTOR;
    }

    public DyeColor getPipeColor() {
        return getCachedState().getBlock() instanceof ItemPipeBlock pipe
                ? pipe.getPipeColor() : DyeColor.WHITE;
    }

    public ItemStack getFilter(int slot) { return whitelist.get(slot); }
    public ItemStack getMovingStack() { return movingStack; }
    public Direction getTravelFrom() { return travelFrom; }
    public Direction getTravelTo() { return travelTo; }
    public ItemStack takeMovingStack() {
        ItemStack dropped = movingStack.copy();
        movingStack = ItemStack.EMPTY;
        route = List.of();
        destinationStorage = null;
        markDirty();
        return dropped;
    }
    public float getTravelProgress(float tickDelta) {
        return Math.min(1.0F, (travelTicks + tickDelta) / TICKS_PER_PIPE);
    }

    public void setFilter(int slot, ItemStack stack) {
        whitelist.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        changed();
    }

    public boolean accepts(ItemStack stack) {
        boolean empty = true;
        for (ItemStack filter : whitelist) {
            if (filter.isEmpty()) continue;
            empty = false;
            if (ItemStack.areItemsAndComponentsEqual(filter, stack)) return true;
        }
        return empty;
    }

    /** Starts one stack on a shortest route from an input to a compatible output. */
    public void transferOneStack() {
        if (!(world instanceof ServerWorld serverWorld)) return;
        Network network = scanNetwork(serverWorld);
        for (Endpoint source : network.inputs) {
            if (!source.pipe.movingStack.isEmpty()) continue;
            for (int slot : slots(source.inventory, source.inventorySide)) {
                ItemStack sourceStack = source.inventory.getStack(slot);
                if (sourceStack.isEmpty() || !source.pipe.accepts(sourceStack)
                        || !canExtract(source.inventory, slot, sourceStack, source.inventorySide)) continue;

                int requested = Math.min(sourceStack.getCount(), sourceStack.getMaxCount());
                for (Endpoint output : network.outputs) {
                    if (output.inventory == source.inventory || output.storagePos.equals(source.storagePos)
                            || !output.pipe.accepts(sourceStack)) continue;
                    int amount = insertCapacity(output.inventory, sourceStack, requested, output.inventorySide);
                    if (amount <= 0) continue;
                    List<BlockPos> path = findPath(serverWorld, source.pipe.pos, output.pipe.pos, getPipeColor());
                    if (path.isEmpty()) continue;

                    ItemStack extracted = sourceStack.split(amount);
                    source.inventory.markDirty();
                    source.pipe.beginTransit(extracted, path, 0, output.storagePos, source.storagePos);
                    serverWorld.playSound(null, source.pipe.pos, SoundEvents.ENTITY_ITEM_PICKUP,
                            SoundCategory.BLOCKS, 0.35F, 1.6F);
                    return; // A redstone pulse launches at most one stack.
                }
            }
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, ItemPipeBlockEntity pipe) {
        if (!(world instanceof ServerWorld serverWorld) || pipe.movingStack.isEmpty()) return;
        if (pipe.travelTicks < TICKS_PER_PIPE) {
            pipe.travelTicks++;
            pipe.changed();
            if (pipe.travelTicks < TICKS_PER_PIPE) return;
        }

        if (pipe.routeIndex + 1 < pipe.route.size()) {
            BlockPos nextPos = pipe.route.get(pipe.routeIndex + 1);
            if (!serverWorld.isChunkLoaded(nextPos)) return;
            if (!(serverWorld.getBlockEntity(nextPos) instanceof ItemPipeBlockEntity next)
                    || !next.movingStack.isEmpty() || next.getPipeColor() != pipe.getPipeColor()) return;
            ItemStack payload = pipe.movingStack;
            next.beginTransit(payload, pipe.route, pipe.routeIndex + 1, pipe.destinationStorage, pipe.pos);
            pipe.clearTransit();
            return;
        }

        if (pipe.destinationStorage == null || !serverWorld.isChunkLoaded(pipe.destinationStorage)) return;
        Inventory destination = HopperBlockEntity.getInventoryAt(serverWorld, pipe.destinationStorage);
        if (destination == null) return;
        Direction pipeToStorage = directionBetween(pipe.pos, pipe.destinationStorage);
        int inserted = insert(destination, pipe.movingStack, pipe.movingStack.getCount(), pipeToStorage.getOpposite());
        if (inserted <= 0) return;
        pipe.movingStack.decrement(inserted);
        if (pipe.movingStack.isEmpty()) {
            serverWorld.playSound(null, pipe.pos, SoundEvents.ENTITY_ITEM_PICKUP,
                    SoundCategory.BLOCKS, 0.25F, 1.9F);
            pipe.clearTransit();
        } else {
            pipe.changed();
        }
    }

    private void beginTransit(ItemStack stack, List<BlockPos> route, int routeIndex,
                              BlockPos destinationStorage, BlockPos previousPos) {
        this.movingStack = stack.copy();
        this.route = List.copyOf(route);
        this.routeIndex = routeIndex;
        this.destinationStorage = destinationStorage.toImmutable();
        this.travelFrom = directionBetween(pos, previousPos);
        BlockPos next = routeIndex + 1 < route.size() ? route.get(routeIndex + 1) : destinationStorage;
        this.travelTo = directionBetween(pos, next);
        this.travelTicks = 0;
        changed();
    }

    private void clearTransit() {
        movingStack = ItemStack.EMPTY;
        route = List.of();
        routeIndex = 0;
        destinationStorage = null;
        travelTicks = 0;
        changed();
    }

    private Network scanNetwork(ServerWorld world) {
        List<Endpoint> inputs = new ArrayList<>();
        List<Endpoint> outputs = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        DyeColor networkColor = getPipeColor();
        queue.add(pos);

        while (!queue.isEmpty() && visited.size() < MAX_NETWORK_PIPES) {
            BlockPos pipePos = queue.remove();
            if (!visited.add(pipePos)) continue;
            if (!(world.getBlockState(pipePos).getBlock() instanceof ItemPipeBlock block)
                    || block.getPipeColor() != networkColor
                    || !(world.getBlockEntity(pipePos) instanceof ItemPipeBlockEntity pipe)) continue;

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pipePos.offset(direction);
                if (world.getBlockState(neighbor).getBlock() instanceof ItemPipeBlock adjacentPipe) {
                    if (block.connectsTo(adjacentPipe) && !visited.contains(neighbor)) queue.add(neighbor);
                    continue;
                }
                if (block.getRole() == ItemPipeBlock.Role.CONNECTOR) continue;
                Inventory inventory = HopperBlockEntity.getInventoryAt(world, neighbor);
                if (inventory == null) continue;
                Endpoint endpoint = new Endpoint(pipe, inventory, neighbor, direction, direction.getOpposite());
                if (block.getRole() == ItemPipeBlock.Role.INPUT) inputs.add(endpoint);
                else outputs.add(endpoint);
            }
        }
        return new Network(inputs, outputs);
    }

    private static List<BlockPos> findPath(ServerWorld world, BlockPos start, BlockPos goal, DyeColor color) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_PIPES) {
            BlockPos current = queue.remove();
            if (current.equals(goal)) {
                ArrayDeque<BlockPos> path = new ArrayDeque<>();
                for (BlockPos at = current; at != null; at = previous.get(at)) path.addFirst(at.toImmutable());
                return List.copyOf(path);
            }
            if (!(world.getBlockState(current).getBlock() instanceof ItemPipeBlock currentPipe)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos next = current.offset(direction);
                if (visited.contains(next)) continue;
                if (world.getBlockState(next).getBlock() instanceof ItemPipeBlock nextPipe
                        && currentPipe.connectsTo(nextPipe) && nextPipe.getPipeColor() == color) {
                    visited.add(next);
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }
        return List.of();
    }

    private static Direction directionBetween(BlockPos from, BlockPos to) {
        for (Direction direction : Direction.values()) if (from.offset(direction).equals(to)) return direction;
        return Direction.NORTH;
    }

    private static int[] slots(Inventory inventory, Direction side) {
        if (inventory instanceof SidedInventory sided) return sided.getAvailableSlots(side);
        int[] slots = new int[inventory.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    private static boolean canExtract(Inventory inventory, int slot, ItemStack stack, Direction side) {
        return !(inventory instanceof SidedInventory sided) || sided.canExtract(slot, stack, side);
    }

    private static boolean canInsert(Inventory inventory, int slot, ItemStack stack, Direction side) {
        return inventory.isValid(slot, stack)
                && (!(inventory instanceof SidedInventory sided) || sided.canInsert(slot, stack, side));
    }

    private static int insertCapacity(Inventory inventory, ItemStack template, int limit, Direction side) {
        int capacity = 0;
        for (int slot : slots(inventory, side)) {
            ItemStack present = inventory.getStack(slot);
            if (!canInsert(inventory, slot, template, side)) continue;
            if (present.isEmpty()) capacity += Math.min(template.getMaxCount(), inventory.getMaxCount(template));
            else if (ItemStack.areItemsAndComponentsEqual(present, template))
                capacity += Math.max(0, Math.min(present.getMaxCount(), inventory.getMaxCount(present)) - present.getCount());
            if (capacity >= limit) return limit;
        }
        return Math.min(capacity, limit);
    }

    private static int insert(Inventory inventory, ItemStack template, int amount, Direction side) {
        int remaining = amount;
        for (int slot : slots(inventory, side)) {
            ItemStack present = inventory.getStack(slot);
            if (!present.isEmpty() && ItemStack.areItemsAndComponentsEqual(present, template)
                    && canInsert(inventory, slot, template, side)) {
                int max = Math.min(present.getMaxCount(), inventory.getMaxCount(present));
                int inserted = Math.min(remaining, max - present.getCount());
                if (inserted > 0) { present.increment(inserted); remaining -= inserted; }
                if (remaining == 0) break;
            }
        }
        for (int slot : slots(inventory, side)) {
            if (remaining == 0) break;
            if (inventory.getStack(slot).isEmpty() && canInsert(inventory, slot, template, side)) {
                int inserted = Math.min(remaining, Math.min(template.getMaxCount(), inventory.getMaxCount(template)));
                inventory.setStack(slot, template.copyWithCount(inserted));
                remaining -= inserted;
            }
        }
        if (remaining != amount) inventory.markDirty();
        return amount - remaining;
    }

    private void changed() {
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        NbtList filters = new NbtList();
        for (int i = 0; i < whitelist.size(); i++) {
            if (whitelist.get(i).isEmpty()) continue;
            NbtCompound entry = new NbtCompound();
            entry.putInt("slot", i);
            entry.put("stack", whitelist.get(i).encode(registries));
            filters.add(entry);
        }
        nbt.put("whitelist", filters);
        if (!movingStack.isEmpty()) {
            nbt.put("moving_stack", movingStack.encode(registries));
            nbt.putLongArray("route", route.stream().mapToLong(BlockPos::asLong).toArray());
            nbt.putInt("route_index", routeIndex);
            if (destinationStorage != null) nbt.putLong("destination", destinationStorage.asLong());
            nbt.putInt("travel_from", travelFrom.getId());
            nbt.putInt("travel_to", travelTo.getId());
            nbt.putInt("travel_ticks", travelTicks);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        for (int i = 0; i < whitelist.size(); i++) whitelist.set(i, ItemStack.EMPTY);
        NbtList filters = nbt.getList("whitelist", 10);
        for (int i = 0; i < filters.size(); i++) {
            NbtCompound entry = filters.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot >= 0 && slot < whitelist.size())
                whitelist.set(slot, ItemStack.fromNbtOrEmpty(registries, entry.getCompound("stack")));
        }

        movingStack = ItemStack.EMPTY;
        route = List.of();
        destinationStorage = null;
        travelTicks = 0;
        if (nbt.contains("moving_stack", 10)) {
            movingStack = ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("moving_stack"));
            long[] routeData = nbt.getLongArray("route");
            List<BlockPos> loadedRoute = new ArrayList<>(routeData.length);
            for (long packed : routeData) loadedRoute.add(BlockPos.fromLong(packed));
            route = List.copyOf(loadedRoute);
            routeIndex = nbt.getInt("route_index");
            if (nbt.contains("destination")) destinationStorage = BlockPos.fromLong(nbt.getLong("destination"));
            travelFrom = Direction.byId(nbt.getInt("travel_from"));
            travelTo = Direction.byId(nbt.getInt("travel_to"));
            travelTicks = nbt.getInt("travel_ticks");
        }
    }

    @Override public Text getDisplayName() {
        return Text.translatable("container.bd.item_pipe_" + getRole().name().toLowerCase());
    }

    @Nullable
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return getRole() == ItemPipeBlock.Role.CONNECTOR ? null : new ItemPipeScreenHandler(syncId, playerInventory, this);
    }

    @Override public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) { return createNbt(registries); }

    private record Endpoint(ItemPipeBlockEntity pipe, Inventory inventory, BlockPos storagePos,
                            Direction pipeToStorage, Direction inventorySide) {}
    private record Network(List<Endpoint> inputs, List<Endpoint> outputs) {}
}
