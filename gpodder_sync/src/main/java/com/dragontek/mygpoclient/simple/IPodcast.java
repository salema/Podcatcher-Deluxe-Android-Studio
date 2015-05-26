package com.dragontek.mygpoclient.simple;

public interface IPodcast {
    String getUrl();

    String getTitle();

    void setTitle(String title);

    String getDescription();

    void setDescription(String description);

    String getLogoUrl();

    void setLogoUrl(String logourl);
}
