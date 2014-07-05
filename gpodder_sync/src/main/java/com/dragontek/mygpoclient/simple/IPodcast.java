package com.dragontek.mygpoclient.simple;

public interface IPodcast {
    public String getUrl();

    public String getTitle();

    public void setTitle(String title);

    public String getDescription();

    public void setDescription(String description);

    public String getLogoUrl();

    public void setLogoUrl(String logourl);
}
