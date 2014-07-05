package com.dragontek.mygpoclient.pub;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ClientConfig {
    public Map<String, String> mygpo;
    @SerializedName("mygpo-feedservice")
    public Map<String, String> mygpo_feedservice;
    public long update_timeout;

}
