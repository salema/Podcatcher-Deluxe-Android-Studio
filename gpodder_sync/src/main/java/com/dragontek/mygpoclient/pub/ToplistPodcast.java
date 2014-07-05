package com.dragontek.mygpoclient.pub;

/**
 * Container class for a toplist entry
 * <p/>
 * This class encapsulates the metadata for a podcast in the podcast toplist.
 *
 * @author joshua.mondragon
 */
public class ToplistPodcast {
    /**
     * The feed URL of the podcast
     */
    public String url;
    /**
     * The title of the podcast
     */
    public String title;
    /**
     * The description of the podcast
     */
    public String description;
    /**
     * The current subscriber count
     */
    public int subscribers;
    /**
     * Last week's subscriber count
     */
    public int subscribersLastWeek;

    // TODO: Shouldn't this just extend Podcast?

    public ToplistPodcast(String url, String title, String description,
                          int subscribers, int subscribersLastWeek) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.subscribers = subscribers;
        this.subscribersLastWeek = subscribersLastWeek;
    }
}
