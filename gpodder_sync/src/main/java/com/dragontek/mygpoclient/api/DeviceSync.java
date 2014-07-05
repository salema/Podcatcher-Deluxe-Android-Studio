package com.dragontek.mygpoclient.api;

import com.google.gson.annotations.SerializedName;

public class DeviceSync {
    @SerializedName("synchronized")
    public String[][] synced;
    @SerializedName("not-synchronized")
    public String[] notsynced;
}
