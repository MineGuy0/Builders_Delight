package com.zrollus.bd.Lib.libHelpers;

public class BDLocation {
    public double x, y, z;
    public float yaw, pitch;
    public String worldId;

    public BDLocation(double x, double y, double z, float yaw, float pitch, String worldId) {
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.worldId = worldId;
    }
}