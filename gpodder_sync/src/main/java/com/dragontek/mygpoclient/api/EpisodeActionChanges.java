package com.dragontek.mygpoclient.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for added episode actions
 *
 * @author jmondragon
 * @see EpisodeAction
 */
public class EpisodeActionChanges {

    /**
     * A list of EpisodeAction objects
     */
    public List<EpisodeAction> actions = new ArrayList<EpisodeAction>();
    /**
     * A timestamp value for use in future requests
     */
    public long since;

    public EpisodeActionChanges(List<EpisodeAction> actions, long since) {
        this.actions = actions;
        this.since = since;
    }
}