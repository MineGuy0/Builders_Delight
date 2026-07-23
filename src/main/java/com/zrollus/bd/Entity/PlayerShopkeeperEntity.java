package com.zrollus.bd.Entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** A server-safe player-shaped shop object with a client-rendered player skin. */
public final class PlayerShopkeeperEntity extends PathAwareEntity {
    private static final TrackedData<String> SKIN_NAME = DataTracker.registerData(PlayerShopkeeperEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_UUID = DataTracker.registerData(PlayerShopkeeperEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_TEXTURE = DataTracker.registerData(PlayerShopkeeperEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_SIGNATURE = DataTracker.registerData(PlayerShopkeeperEntity.class, TrackedDataHandlerRegistry.STRING);

    public PlayerShopkeeperEntity(EntityType<? extends PlayerShopkeeperEntity> type, World world) {
        super(type, world);
        setAiDisabled(true);
        setPersistent();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SKIN_NAME, "Steve");
        builder.add(SKIN_UUID, "");
        builder.add(SKIN_TEXTURE, "");
        builder.add(SKIN_SIGNATURE, "");
    }

    public void setSkinProfile(String name, UUID uuid, String texture, String signature) {
        dataTracker.set(SKIN_NAME, name == null || name.isBlank() ? "Steve" : name);
        dataTracker.set(SKIN_UUID, uuid == null ? "" : uuid.toString());
        dataTracker.set(SKIN_TEXTURE, texture == null ? "" : texture);
        dataTracker.set(SKIN_SIGNATURE, signature == null ? "" : signature);
    }

    public String getSkinName() {
        return dataTracker.get(SKIN_NAME);
    }

    public GameProfile getSkinProfile() {
        String name = getSkinName();
        UUID uuid;
        try {
            uuid = UUID.fromString(dataTracker.get(SKIN_UUID));
        } catch (IllegalArgumentException ignored) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }
        GameProfile profile = new GameProfile(uuid, name);
        String texture = dataTracker.get(SKIN_TEXTURE);
        if (!texture.isEmpty()) {
            String signature = dataTracker.get(SKIN_SIGNATURE);
            profile.getProperties().put("textures", signature.isEmpty()
                    ? new Property("textures", texture)
                    : new Property("textures", texture, signature));
        }
        return profile;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SkinName", dataTracker.get(SKIN_NAME));
        nbt.putString("SkinUuid", dataTracker.get(SKIN_UUID));
        nbt.putString("SkinTexture", dataTracker.get(SKIN_TEXTURE));
        nbt.putString("SkinSignature", dataTracker.get(SKIN_SIGNATURE));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setSkinProfile(
                nbt.contains("SkinName") ? nbt.getString("SkinName") : "Steve",
                parseUuid(nbt.getString("SkinUuid")),
                nbt.getString("SkinTexture"),
                nbt.getString("SkinSignature"));
    }

    private static UUID parseUuid(String value) {
        try {
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
