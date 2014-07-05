package com.dragontek.mygpoclient.feeds;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class FeedServiceResponse extends ArrayList<Feed> {

    public long last_modfied;
    public String[] feed_urls;
    public String[] indexed_feeds = {};

    public FeedServiceResponse(List<Feed> feeds, long last_modified,
                               String[] feed_urls) {
        super.addAll(feeds);
    }

    public long getLastModified() {
        return last_modfied;
    }

    public void getFeeds() {

    }

    public void getFeed() {

    }
}
