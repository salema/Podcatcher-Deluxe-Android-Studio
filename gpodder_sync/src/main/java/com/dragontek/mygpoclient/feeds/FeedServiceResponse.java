package com.dragontek.mygpoclient.feeds;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class FeedServiceResponse extends ArrayList<Feed> {

    public long last_modified;
    public String[] feed_urls;
    public String[] indexed_feeds = {};

    public FeedServiceResponse(List<Feed> feeds, long last_modified,
                               String[] feed_urls) {
        super.addAll(feeds);
    }

    public long getLastModified() {
        return last_modified;
    }

    public void getFeeds() {

    }

    public void getFeed() {

    }
}
