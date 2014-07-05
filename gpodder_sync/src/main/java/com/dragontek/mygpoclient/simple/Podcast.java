package com.dragontek.mygpoclient.simple;

import com.dragontek.mygpoclient.feeds.Feed;

/**
 * Container class for a podcast Encapsulates the metadata for a podcast
 *
 * @author joshua.mondragon
 */
public class Podcast implements IPodcast {
    /**
     * The feed URL of the podcast
     */
    private String url;
    /**
     * The title of the podcast
     */
    private String title;
    /**
     * The description of the podcast
     */
    private String description;
    /**
     * The current subscriber count
     */
    private int subscribers;
    /**
     * Last week's subscriber count
     */
    private int subscribers_last_week;
    /**
     * Link to the website for the podcast
     */
    private String website;
    /**
     * URL for the logo image of the podcast
     */
    private String logo_url;

    public Podcast(String url) {
        this(url, null);
    }

    public Podcast(String url, String title) {
        this(url, title, null);

    }

    public Podcast(String url, String title, String description) {
        this(url, title, description, null);

    }

    public Podcast(String url, String title, String description, String link) {
        this(url, title, description, link, null);
    }

    public Podcast(String url, String title, String description, String link,
                   String logo_url) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.website = link;
        this.logo_url = logo_url;
    }

    public Podcast(Feed feed) {
        this.url = feed.getUrl();
        this.title = feed.getTitle();
        this.description = feed.getDescription();
        this.website = feed.getLink();
        this.logo_url = feed.getLogoUrl();
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return this.website;
    }

    @Override
    public String getLogoUrl() {
        return this.logo_url;
    }

    @Override
    public void setLogoUrl(String logo_url) {
        this.logo_url = logo_url;
    }

    public int getSubscribers() {
        return subscribers;
    }

    public int getSubscribersLastWeek() {
        return subscribers_last_week;
    }

}