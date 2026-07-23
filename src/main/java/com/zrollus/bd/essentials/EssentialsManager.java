package com.zrollus.bd.essentials;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent state shared by the Essentials-style command suite. */
public final class EssentialsManager extends PersistentState {
    public static final class PlayerState {
        public boolean afk;
        public boolean vanished;
        public boolean god;
        public boolean socialSpy;
        public boolean messagesEnabled = true;
        public boolean paymentsEnabled = true;
        public boolean autoTeleport;
        public boolean teleportEnabled = true;
        public long mutedUntil;
        public String muteReason = "";
        public long lastActive = System.currentTimeMillis();
        public final Set<UUID> ignored = new HashSet<>();
        public final Map<String, Long> kitUses = new HashMap<>();
    }

    public record SavedLocation(String world, BlockPos pos, float yaw, float pitch) {
        NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("world", world);
            nbt.putLong("pos", pos.asLong());
            nbt.putFloat("yaw", yaw);
            nbt.putFloat("pitch", pitch);
            return nbt;
        }

        static SavedLocation fromNbt(NbtCompound nbt) {
            return new SavedLocation(nbt.getString("world"), BlockPos.fromLong(nbt.getLong("pos")),
                    nbt.getFloat("yaw"), nbt.getFloat("pitch"));
        }
    }

    public static final class JailRecord {
        public String jail;
        public long until;
        public String reason;

        JailRecord(String jail, long until, String reason) {
            this.jail = jail;
            this.until = until;
            this.reason = reason;
        }
    }

    public static final class Kit {
        public long cooldownSeconds;
        public final List<ItemStack> contents = new ArrayList<>();
    }

    private final Map<UUID, PlayerState> players = new HashMap<>();
    public final Map<String, SavedLocation> jails = new LinkedHashMap<>();
    public final Map<UUID, JailRecord> jailed = new HashMap<>();
    public final Map<String, Kit> kits = new LinkedHashMap<>();
    public final Map<String, Long> worth = new HashMap<>();
    public final List<String> transactions = new ArrayList<>();

    public PlayerState player(UUID id) {
        return players.computeIfAbsent(id, ignored -> new PlayerState());
    }

    public void changed() { markDirty(); }

    public void logTransaction(String line) {
        transactions.add(0, System.currentTimeMillis() + "|" + line);
        while (transactions.size() > 500) transactions.remove(transactions.size() - 1);
        markDirty();
    }

    public boolean isMuted(UUID id) {
        PlayerState state = player(id);
        if (state.mutedUntil == 0) return false;
        if (state.mutedUntil > 0 && state.mutedUntil <= System.currentTimeMillis()) {
            state.mutedUntil = 0;
            state.muteReason = "";
            markDirty();
            return false;
        }
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList playerList = new NbtList();
        for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
            PlayerState state = entry.getValue();
            NbtCompound data = new NbtCompound();
            data.putUuid("id", entry.getKey());
            data.putBoolean("afk", state.afk);
            data.putBoolean("vanished", state.vanished);
            data.putBoolean("god", state.god);
            data.putBoolean("social_spy", state.socialSpy);
            data.putBoolean("messages", state.messagesEnabled);
            data.putBoolean("payments", state.paymentsEnabled);
            data.putBoolean("tp_auto", state.autoTeleport);
            data.putBoolean("tp_enabled", state.teleportEnabled);
            data.putLong("muted_until", state.mutedUntil);
            data.putString("mute_reason", state.muteReason);
            data.putLong("last_active", state.lastActive);
            NbtList ignored = new NbtList();
            for (UUID ignoredId : state.ignored) {
                NbtCompound value = new NbtCompound();
                value.putUuid("id", ignoredId);
                ignored.add(value);
            }
            data.put("ignored", ignored);
            NbtList kitUses = new NbtList();
            for (Map.Entry<String, Long> use : state.kitUses.entrySet()) {
                NbtCompound value = new NbtCompound();
                value.putString("name", use.getKey());
                value.putLong("time", use.getValue());
                kitUses.add(value);
            }
            data.put("kit_uses", kitUses);
            playerList.add(data);
        }
        nbt.put("players", playerList);

        NbtList jailList = new NbtList();
        for (Map.Entry<String, SavedLocation> entry : jails.entrySet()) {
            NbtCompound data = entry.getValue().toNbt();
            data.putString("name", entry.getKey());
            jailList.add(data);
        }
        nbt.put("jails", jailList);

        NbtList jailedList = new NbtList();
        for (Map.Entry<UUID, JailRecord> entry : jailed.entrySet()) {
            NbtCompound data = new NbtCompound();
            data.putUuid("id", entry.getKey());
            data.putString("jail", entry.getValue().jail);
            data.putLong("until", entry.getValue().until);
            data.putString("reason", entry.getValue().reason);
            jailedList.add(data);
        }
        nbt.put("jailed", jailedList);

        NbtList kitList = new NbtList();
        for (Map.Entry<String, Kit> entry : kits.entrySet()) {
            NbtCompound data = new NbtCompound();
            data.putString("name", entry.getKey());
            data.putLong("cooldown", entry.getValue().cooldownSeconds);
            NbtList contents = new NbtList();
            for (ItemStack stack : entry.getValue().contents) contents.add(stack.encodeAllowEmpty(registries));
            data.put("contents", contents);
            kitList.add(data);
        }
        nbt.put("kits", kitList);

        NbtList worthList = new NbtList();
        for (Map.Entry<String, Long> entry : worth.entrySet()) {
            NbtCompound data = new NbtCompound();
            data.putString("item", entry.getKey());
            data.putLong("value", entry.getValue());
            worthList.add(data);
        }
        nbt.put("worth", worthList);
        NbtList history = new NbtList();
        for (String line : transactions) {
            NbtCompound data = new NbtCompound();
            data.putString("line", line);
            history.add(data);
        }
        nbt.put("transactions", history);
        return nbt;
    }

    private static EssentialsManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        EssentialsManager manager = new EssentialsManager();
        NbtList playerList = nbt.getList("players", 10);
        for (int index = 0; index < playerList.size(); index++) {
            NbtCompound data = playerList.getCompound(index);
            PlayerState state = new PlayerState();
            state.afk = data.getBoolean("afk");
            state.vanished = data.getBoolean("vanished");
            state.god = data.getBoolean("god");
            state.socialSpy = data.getBoolean("social_spy");
            state.messagesEnabled = !data.contains("messages") || data.getBoolean("messages");
            state.paymentsEnabled = !data.contains("payments") || data.getBoolean("payments");
            state.autoTeleport = data.getBoolean("tp_auto");
            state.teleportEnabled = !data.contains("tp_enabled") || data.getBoolean("tp_enabled");
            state.mutedUntil = data.getLong("muted_until");
            state.muteReason = data.getString("mute_reason");
            state.lastActive = data.getLong("last_active");
            NbtList ignored = data.getList("ignored", 10);
            for (int i = 0; i < ignored.size(); i++) state.ignored.add(ignored.getCompound(i).getUuid("id"));
            NbtList kitUses = data.getList("kit_uses", 10);
            for (int i = 0; i < kitUses.size(); i++) {
                NbtCompound use = kitUses.getCompound(i);
                state.kitUses.put(use.getString("name"), use.getLong("time"));
            }
            manager.players.put(data.getUuid("id"), state);
        }
        NbtList jailList = nbt.getList("jails", 10);
        for (int i = 0; i < jailList.size(); i++) {
            NbtCompound data = jailList.getCompound(i);
            manager.jails.put(data.getString("name"), SavedLocation.fromNbt(data));
        }
        NbtList jailedList = nbt.getList("jailed", 10);
        for (int i = 0; i < jailedList.size(); i++) {
            NbtCompound data = jailedList.getCompound(i);
            manager.jailed.put(data.getUuid("id"), new JailRecord(data.getString("jail"), data.getLong("until"), data.getString("reason")));
        }
        NbtList kitList = nbt.getList("kits", 10);
        for (int i = 0; i < kitList.size(); i++) {
            NbtCompound data = kitList.getCompound(i);
            Kit kit = new Kit();
            kit.cooldownSeconds = data.getLong("cooldown");
            NbtList contents = data.getList("contents", 10);
            for (int j = 0; j < contents.size(); j++)
                kit.contents.add(ItemStack.fromNbt(registries, contents.get(j)).orElse(ItemStack.EMPTY));
            manager.kits.put(data.getString("name"), kit);
        }
        NbtList worthList = nbt.getList("worth", 10);
        for (int i = 0; i < worthList.size(); i++) {
            NbtCompound data = worthList.getCompound(i);
            manager.worth.put(data.getString("item"), data.getLong("value"));
        }
        NbtList history = nbt.getList("transactions", 10);
        for (int i = 0; i < history.size(); i++) manager.transactions.add(history.getCompound(i).getString("line"));
        return manager;
    }

    public static EssentialsManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(EssentialsManager::new, EssentialsManager::fromNbt, null),
                "bd_essentials");
    }
}
