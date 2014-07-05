package com.dragontek.mygpoclient.api;

import java.util.Arrays;
import java.util.Dictionary;

/**
 * This class encapsulates a podcast device
 *
 * @author jmondragon
 */

public class PodcastDevice {
    final static String[] VALID_TYPES = new String[]{
            "desktop", "laptop",
            "mobile", "server", "other"
    };
    /**
     * The ID used to refer to this device
     */
    public String id;
    /**
     * A user-defined "name" for this device
     */
    public String caption;
    /**
     * A valid type of podcast device (see VALID_TYPES)
     */
    public String type;
    /**
     * The number of podcasts this device is subscribed to
     */
    public int subscriptions;

    public PodcastDevice(String deviceId, String caption, String type) {
        this(deviceId, caption, type, 0);
    }

    public PodcastDevice(String id, String caption, String type,
                         int subscriptions) {
        if (!Arrays.asList(VALID_TYPES).contains(type))
            throw new IllegalArgumentException(String.format(
                    "Invalid device type '%s' (see VALID_TYPES)", type));

        this.id = id;
        this.caption = caption;
        this.type = type;
        this.subscriptions = subscriptions;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s, %s, %s)", this.getClass()
                        .getSimpleName(), this.id, this.caption, this.type,
                this.subscriptions
        );
    }

    public PodcastDevice fromDictionary(Dictionary<String, String> m) {
        return new PodcastDevice(m.get("id"), m.get("caption"), m.get("type"),
                Integer.parseInt(m.get("subscriptions")));
    }
}
