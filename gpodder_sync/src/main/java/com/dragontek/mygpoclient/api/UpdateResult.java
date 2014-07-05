package com.dragontek.mygpoclient.api;

import java.util.Map;

/**
 * Container for subscription update results
 *
 * @author jmondragon
 */
public class UpdateResult {

    /**
     * A list of (old_url, new_url) tuples
     */
    public Map<String, String> updateUrls;
    /**
     * A timestamp value for use in future requests
     */
    public long timestamp;

    public UpdateResult(Map<String, String> updateUrls, long timestamp) {
        this.updateUrls = updateUrls;
        this.timestamp = timestamp;
    }
}