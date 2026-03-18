package com.zrollus.bd.Lib.libHelpers;

import java.util.HashMap;
import java.util.Map;

public class PlayerDataModel {
    public BDLocation lastLocation; // For /back
    public Map<String, BDLocation> homes = new HashMap<>(); // For /home [name]
    public boolean tpaEnabled = true; // Default to true
    public long bal = 0; // Default to true
    public String lastKnownName = ""; // Default to nothing
}