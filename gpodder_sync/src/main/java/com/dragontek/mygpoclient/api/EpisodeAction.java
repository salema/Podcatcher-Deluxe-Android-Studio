package com.dragontek.mygpoclient.api;

import java.util.Arrays;

/**
 * This class encapsulates an episode action
 *
 * @author jmondragon
 */
public class EpisodeAction {
    final static String[] VALID_ACTIONS = new String[]{
            "download", "play",
            "delete", "new"
    };
    /**
     * The feed URL of the podcast
     */
    public String podcast;
    /**
     * The enclosure URL or GUID of the episode
     */
    public String episode;
    /**
     * One of 'download', 'play', 'delete' or 'new'
     */
    public String action;
    /**
     * The device_id on which the action has taken place
     */
    public String device;
    /**
     * When the action took place (in XML time format)
     */
    public String timestamp;
    /**
     * The start time of a play event in seconds
     */
    public Integer started = null;
    /**
     * The current position of a play event in seconds
     */
    public Integer position = null;
    /**
     * The total time of the episode (for play events)
     */
    public Integer total = null;

    public EpisodeAction(String podcast, String episode, String action,
                         String device, String timestamp, Integer started, Integer position,
                         Integer total) {
        // Check if the action is valid
        if (!Arrays.asList(VALID_ACTIONS).contains(action))
            throw new IllegalArgumentException(String.format(
                    "Invalid action '%s' (see VALID_ACTIONS)", action));

        // Disallow play-only attributes for non-play actions
        if (!action.equals("play")) {
            if (started != null)
                throw new IllegalArgumentException(
                        "Started can only be set for the 'play' action");
            if (position != null)
                throw new IllegalArgumentException(
                        "Position can only be set for the 'play' action");
            if (total != null)
                throw new IllegalArgumentException(
                        "Total can only be set for the 'play' action");
        }

        // Check the format of the timestamp value
        if (timestamp != null) {
            // if util.iso8601_to_datetime(timestamp) is None:
            // raise ValueError('Timestamp has to be in ISO 8601 format but was
            // %s' % timestamp)
        }

        // Check if we have a "position" value if we have started or total
        if (position != null && (started != null | total != null))
            throw new IllegalArgumentException(
                    "Started or total set, but no position given");

        this.podcast = podcast;
        this.episode = episode;
        this.action = action;
        this.device = device;
        this.timestamp = timestamp;
        this.started = started;
        this.position = position;
        this.total = total;

    }

    @Override
    public String toString() {
        return timestamp + ": " + action +
                (position != null ? " to " + position + "s " : " ") +
                episode + "(" + podcast + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof EpisodeAction))
            return false;

        EpisodeAction other = (EpisodeAction) o;

        return podcast.equals(other.podcast) &&
                episode.equals(other.episode) &&
                action.equals(other.action) &&
                timestamp.equals(other.timestamp);
    }

    @Override
    public int hashCode() {
        return 42 + (podcast == null ? 0 : podcast.hashCode()) +
                (episode == null ? 0 : episode.hashCode()) +
                (action == null ? 0 : action.hashCode()) +
                (timestamp == null ? 0 : timestamp.hashCode());
    }
}
