package com.dragontek.mygpoclient.api;

import java.util.Set;

/**
 * Container for subscription changes
 *
 * @author jmondragon
 */
public class SubscriptionChanges {

    /**
     * A list of URLs that have been added
     */
    public Set<String> add;
    /**
     * A list of URLs that have been removed
     */
    public Set<String> remove;
    /**
     * A timestamp value for use in future requests
     */
    public long timestamp;

    public SubscriptionChanges(Set<String> add, Set<String> remove) {
        this(add, remove, 0L); // or should timestamp be now?
    }

    public SubscriptionChanges(Set<String> add, Set<String> remove,
                               long timestamp) {
        this.add = add;
        this.remove = remove;
        this.timestamp = timestamp;
    }
}