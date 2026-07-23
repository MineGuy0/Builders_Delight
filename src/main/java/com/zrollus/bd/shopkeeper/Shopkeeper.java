package com.zrollus.bd.shopkeeper;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Shopkeeper {
    public enum Type { ADMIN, SELLING, BUYING, TRADING, BOOK }
    public enum AccessLevel {
        NONE(0), CONTAINER(10), EDIT(20), FULL(99);

        private final int weight;
        AccessLevel(int weight) { this.weight = weight; }
        public boolean includes(AccessLevel required) { return weight >= required.weight; }
    }

    public final UUID id;
    public UUID owner;
    public UUID hireSeller;
    public String ownerName;
    public String name;
    private Text styledName;
    public Type type;
    public String objectType;
    public String skinName = "";
    public UUID skinUuid;
    public String skinTexture = "";
    public String skinSignature = "";
    public UUID entityId;
    public String worldId;
    public BlockPos objectPos;
    public String containerWorldId;
    public BlockPos containerPos;
    public boolean signObject;
    public boolean closed;
    public boolean silent;
    public boolean notifyOwner = true;
    public boolean exactItems = true;
    public long hirePrice = -1;
    public int tradePermissionLevel;
    public NbtCompound objectData = new NbtCompound();
    public final Map<UUID, AccessLevel> members = new HashMap<>();
    public final List<ShopkeeperTrade> trades = new ArrayList<>();
    public final List<ShopkeeperSnapshot> snapshots = new ArrayList<>();

    public Shopkeeper(UUID id) {
        this.id = id;
    }

    public boolean isAdmin() { return type == Type.ADMIN; }

    public void setName(Text newName) {
        name = newName == null ? "Shopkeeper" : newName.getString();
        styledName = newName == null ? null : newName.copy();
    }

    public MutableText getDisplayName() {
        if (styledName != null && styledName.getString().equals(name)) return styledName.copy();
        return Text.literal(name == null || name.isBlank() ? "Shopkeeper" : name);
    }

    public boolean canAccess(UUID player, boolean op, AccessLevel required) {
        return op || (owner != null && owner.equals(player))
                || (hireSeller != null && hireSeller.equals(player))
                || members.getOrDefault(player, AccessLevel.NONE).includes(required);
    }

    public boolean canManage(UUID player, boolean op) { return canAccess(player, op, AccessLevel.EDIT); }
    public boolean canFullyManage(UUID player, boolean op) { return canAccess(player, op, AccessLevel.FULL); }
    public boolean canAccessContainer(UUID player, boolean op) { return canAccess(player, op, AccessLevel.CONTAINER); }
    public boolean isOwnerOrMember(UUID player) {
        return (owner != null && owner.equals(player)) || members.containsKey(player);
    }

    NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("id", id);
        if (owner != null) nbt.putUuid("owner", owner);
        if (hireSeller != null) nbt.putUuid("hire_seller", hireSeller);
        nbt.putString("owner_name", ownerName == null ? "Server" : ownerName);
        nbt.putString("name", name == null ? "Shopkeeper" : name);
        if (styledName != null && styledName.getString().equals(name)) {
            nbt.putString("styled_name", Text.Serialization.toJsonString(styledName, registries));
        }
        nbt.putString("type", type.name());
        nbt.putString("object_type", objectType == null ? "minecraft:villager" : objectType);
        if (!skinName.isEmpty()) nbt.putString("skin_name", skinName);
        if (skinUuid != null) nbt.putUuid("skin_uuid", skinUuid);
        if (!skinTexture.isEmpty()) nbt.putString("skin_texture", skinTexture);
        if (!skinSignature.isEmpty()) nbt.putString("skin_signature", skinSignature);
        if (entityId != null) nbt.putUuid("entity", entityId);
        nbt.putString("world", worldId);
        nbt.putLong("object_pos", objectPos.asLong());
        nbt.putString("container_world", containerWorldId == null ? worldId : containerWorldId);
        if (containerPos != null) nbt.putLong("container_pos", containerPos.asLong());
        nbt.putBoolean("has_container", containerPos != null);
        nbt.putBoolean("sign", signObject);
        nbt.putBoolean("closed", closed);
        nbt.putBoolean("silent", silent);
        nbt.putBoolean("notify", notifyOwner);
        nbt.putBoolean("exact", exactItems);
        nbt.putLong("hire", hirePrice);
        nbt.putInt("trade_permission_level", tradePermissionLevel);
        if (!objectData.isEmpty()) nbt.put("object_data", objectData.copy());

        NbtList memberList = new NbtList();
        for (Map.Entry<UUID, AccessLevel> member : members.entrySet()) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("id", member.getKey());
            entry.putString("access", member.getValue().name());
            memberList.add(entry);
        }
        nbt.put("members", memberList);

        NbtList tradeList = new NbtList();
        for (ShopkeeperTrade trade : trades) tradeList.add(trade.toNbt(registries));
        nbt.put("trades", tradeList);
        NbtList snapshotList = new NbtList();
        for (ShopkeeperSnapshot snapshot : snapshots) snapshotList.add(snapshot.toNbt(registries));
        nbt.put("snapshots", snapshotList);
        return nbt;
    }

    static Shopkeeper fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        Shopkeeper shop = new Shopkeeper(nbt.getUuid("id"));
        if (nbt.containsUuid("owner")) shop.owner = nbt.getUuid("owner");
        if (nbt.containsUuid("hire_seller")) shop.hireSeller = nbt.getUuid("hire_seller");
        shop.ownerName = nbt.getString("owner_name");
        shop.name = nbt.getString("name");
        if (nbt.contains("styled_name")) {
            try {
                Text loadedName = Text.Serialization.fromJson(nbt.getString("styled_name"), registries);
                if (loadedName != null && loadedName.getString().equals(shop.name)) shop.styledName = loadedName;
            } catch (RuntimeException ignored) {
                // Keep compatibility with shops saved before styled names were supported.
            }
        }
        try { shop.type = Type.valueOf(nbt.getString("type")); }
        catch (IllegalArgumentException ignored) { shop.type = Type.TRADING; }
        shop.objectType = nbt.getString("object_type");
        shop.skinName = nbt.getString("skin_name");
        if (nbt.containsUuid("skin_uuid")) shop.skinUuid = nbt.getUuid("skin_uuid");
        shop.skinTexture = nbt.getString("skin_texture");
        shop.skinSignature = nbt.getString("skin_signature");
        if (nbt.containsUuid("entity")) shop.entityId = nbt.getUuid("entity");
        shop.worldId = nbt.getString("world");
        shop.objectPos = BlockPos.fromLong(nbt.getLong("object_pos"));
        shop.containerWorldId = nbt.getString("container_world");
        if (nbt.getBoolean("has_container")) shop.containerPos = BlockPos.fromLong(nbt.getLong("container_pos"));
        shop.signObject = nbt.getBoolean("sign");
        shop.closed = nbt.getBoolean("closed");
        shop.silent = nbt.getBoolean("silent");
        shop.notifyOwner = nbt.getBoolean("notify");
        shop.exactItems = !nbt.contains("exact") || nbt.getBoolean("exact");
        shop.hirePrice = nbt.contains("hire") ? nbt.getLong("hire") : -1;
        shop.tradePermissionLevel = nbt.getInt("trade_permission_level");
        if (nbt.contains("object_data")) shop.objectData = nbt.getCompound("object_data").copy();

        NbtList memberList = nbt.getList("members", 10);
        for (int i = 0; i < memberList.size(); i++) {
            NbtCompound entry = memberList.getCompound(i);
            AccessLevel access = AccessLevel.FULL; // Migration for shops saved before access levels.
            if (entry.contains("access")) {
                try { access = AccessLevel.valueOf(entry.getString("access")); }
                catch (IllegalArgumentException ignored) {}
            }
            shop.members.put(entry.getUuid("id"), access);
        }
        NbtList tradeList = nbt.getList("trades", 10);
        for (int i = 0; i < tradeList.size(); i++) shop.trades.add(ShopkeeperTrade.fromNbt(tradeList.getCompound(i), registries));
        NbtList snapshotList = nbt.getList("snapshots", 10);
        for (int i = 0; i < snapshotList.size(); i++) shop.snapshots.add(ShopkeeperSnapshot.fromNbt(snapshotList.getCompound(i), registries));
        return shop;
    }
}
