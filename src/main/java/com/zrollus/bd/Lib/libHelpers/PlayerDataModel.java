package com.zrollus.bd.Lib.libHelpers;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.time.LocalDateTime;
import java.util.*;

public class PlayerDataModel {
    public List<String> mail = new ArrayList<>();
    public BDLocation lastLocation;
    public Map<String, BDLocation> homes = new HashMap<>();
    public boolean tpaEnabled = true;
    public long bal = 0;
    public String lastKnownName = "";
    public Vec3d coords;
    public String worldId;
    public float lastYaw;
    public float lastPitch;
    public String lastLogonTime;
    public String nickname = "";
    public UUID lastMessenger = null;
    public Map<String, String> playerVaults = new HashMap<>();
}