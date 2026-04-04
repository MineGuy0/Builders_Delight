package com.zrollus.bd.Lib.libHelpers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminStateRegistry {
    // Players in this set can bypass Shop Chest protections
    public static final Set<UUID> BYPASS_MODE = new HashSet<>();
}