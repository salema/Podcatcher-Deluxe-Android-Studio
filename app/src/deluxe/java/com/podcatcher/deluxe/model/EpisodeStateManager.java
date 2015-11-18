/**
 * Copyright 2012-2015 Kevin Hausmann
 *
 * This file is part of Podcatcher Deluxe.
 *
 * Podcatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Podcatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Podcatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package com.podcatcher.deluxe.model;

import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.listeners.OnChangeEpisodeStateListener;
import com.podcatcher.deluxe.listeners.OnChangePodcastListListener;
import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

import android.os.Process;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * This part of the episode manager stack handles the actual episode state,
 * including old/new flagging and resume times.
 *
 * @see EpisodeManager
 */
public abstract class EpisodeStateManager extends EpisodePlaylistManager implements
        OnLoadPodcastListener, OnChangePodcastListListener {

    /**
     * The call-back set for the episode state changed listeners
     */
    private Set<OnChangeEpisodeStateListener> stateListeners = new HashSet<>();

    /**
     * Helper to prevent clean-up from running too often
     */
    private int podcastLoadCounter = 0;
    /**
     * Helper to prevent clean-up to run twice for the same podcast
     */
    private Set<String> podcastsCleanUpRanFor = new HashSet<>();

    /**
     * Init the episode state manager.
     *
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodeStateManager(Podcatcher app) {
        super(app);
    }

    @Override
    public void onEpisodeMetadataLoaded(Map<String, EpisodeMetadata> metadata) {
        super.onEpisodeMetadataLoaded(metadata);

        // We register to be alerted on podcast loads and podcast list changes
        // so we can perform some clean-ups
        PodcastManager.getInstance().addLoadPodcastListener(this);
        PodcastManager.getInstance().addChangePodcastListListener(this);
    }

    /**
     * Set the old/new state for an episode.
     *
     * @param episode Episode to set state for (not <code>null</code>).
     * @param isOld   State to set, i.e. <code>true</code> if the episode is now
     *                'old' and <code>false</code> otherwise. Give <code>null</code>
     *                to reset the value to the default.
     */
    public void setState(Episode episode, Boolean isOld) {
        if (episode != null && episode.getMediaUrl() != null && metadata != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            // Metadata not yet created
            if (meta == null && isOld != null && isOld) {
                meta = new EpisodeMetadata();
                meta.isOld = true;

                metadata.put(episode.getMediaUrl(), meta);
            } // Metadata available
            else if (meta != null)
                // We do not need to set this if false, simply remove the record
                meta.isOld = (isOld != null && isOld ? true : null);

            // We need to add the podcast URL to decide whether this meta
            // information is still needed later (Once the podcast feed is
            // deleted or the episode is not in the feed anymore, we can delete
            // the metadata for the episode).
            if (meta != null && meta.isOld != null && episode.getPodcast() != null)
                meta.podcastUrl = episode.getPodcast().getUrl();

            // Mark metadata record as dirty
            metadataChanged = true;

            // Alert listeners
            for (OnChangeEpisodeStateListener listener : stateListeners)
                listener.onStateChanged(episode, isOld == null ? false : isOld);
        }
    }

    /**
     * Get the state information for an episode.
     *
     * @param episode Episode to get old/new state for.
     * @return The state: <code>true</code> if the episode is marked old,
     * <code>false</code> otherwise.
     */
    public boolean getState(Episode episode) {
        if (episode != null && episode.getMediaUrl() != null && metadata != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            if (meta != null && meta.isOld != null)
                return meta.isOld;
        }

        return false;
    }

    /**
     * Count the number of episodes not marked old for given podcast.
     *
     * @param podcast Podcast to count for.
     * @return The number of new episode in the podcast.
     */
    public int getNewEpisodeCount(Podcast podcast) {
        int count = 0;

        if (podcast != null)
            for (Episode episode : podcast.getEpisodes())
                if (!getState(episode))
                    count++;

        return count;
    }

    /**
     * Add a state changed listener.
     *
     * @param listener Listener to add.
     * @see OnChangeEpisodeStateListener
     */
    public void addStateChangedListener(OnChangeEpisodeStateListener listener) {
        stateListeners.add(listener);
    }

    /**
     * Remove a download listener.
     *
     * @param listener Listener to remove.
     * @see OnChangeEpisodeStateListener
     */
    public void removeStateChangedListener(OnChangeEpisodeStateListener listener) {
        stateListeners.remove(listener);
    }

    /**
     * Set the resume time meta data field for an episode.
     *
     * @param episode Episode to set resume time for.
     * @param at      Time in millis from the start of the episode's media file to
     *                resume playback from. Give <code>null</code> to reset.
     */
    public void setResumeAt(Episode episode, Integer at) {
        if (episode != null && episode.getMediaUrl() != null && !episode.isLive() && metadata != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            // Metadata not yet created
            if (meta == null && at != null) {
                meta = new EpisodeMetadata();
                meta.resumeAt = at;

                metadata.put(episode.getMediaUrl(), meta);
            } // Metadata available
            else if (meta != null)
                meta.resumeAt = at;

            // We need to add the podcast URL to decide whether this meta
            // information is still needed later (Once the podcast feed is
            // deleted or the episode is not in the feed anymore, we can delete
            // the metadata for the episode).
            if (meta != null && meta.resumeAt != null && episode.getPodcast() != null)
                meta.podcastUrl = episode.getPodcast().getUrl();

            // Mark metadata record as dirty
            metadataChanged = true;

            // Alert listeners
            for (OnChangeEpisodeStateListener listener : stateListeners)
                listener.onResumeAtChanged(episode, at);
        }
    }

    /**
     * Get the resume time meta data field for an episode.
     *
     * @param episode Episode to get resume time for.
     * @return The resume time as millis from the start or zero if not set.
     */
    public int getResumeAt(Episode episode) {
        if (episode != null && episode.getMediaUrl() != null && metadata != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            if (meta != null && meta.resumeAt != null)
                return meta.resumeAt;
        }

        return 0;
    }

    public void onPodcastAdded(Podcast podcast) {
        // pass
    }

    @Override
    public void onPodcastRemoved(final Podcast podcast) {
        if (podcast != null) {
            // Go off the main thread, we rely on getting an iterator from the
            // metadata being thread safe here!
            new Thread() {

                @Override
                public void run() {
                    Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

                    // Clean all state meta data information for episodes of the deleted feed
                    for (Entry<String, EpisodeMetadata> entry : metadata.entrySet()) {
                        // Find metadata records with matching podcast
                        if (podcast.equalByUrl(entry.getValue().podcastUrl) &&
                                entry.getValue().hasOnlyStateData()) {
                            // This is actually enough since the task storing
                            // the metadata will clean empty records
                            entry.getValue().isOld = null;
                            entry.getValue().resumeAt = null;
                        }
                    }
                }
            }.start();
        }
    }

    @Override
    public void onPodcastMoved(Podcast podcast, String newUrl) {
        // TODO Move metadata over
    }

    @Override
    public void onPodcastLoaded(final Podcast podcast) {
        // We do not want to run this too frequently and for all podcasts at
        // once. In addition it should run only once per podcast during the
        // lifetime of this EpisodeManager
        if (podcast != null && podcast.getEpisodeCount() > 0 &&
                !podcastsCleanUpRanFor.contains(podcast.getUrl()) &&
                podcastLoadCounter % 10 == 0) {
            // Update helpers
            podcastLoadCounter++;
            podcastsCleanUpRanFor.add(podcast.getUrl());

            // Go off the main thread, we rely on getting an iterator from the
            // metadata being thread safe here!
            new Thread() {

                @Override
                public void run() {
                    Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

                    // Clean all state meta data information for episodes no
                    // longer present in the podcast feed
                    for (Entry<String, EpisodeMetadata> entry : metadata.entrySet()) {
                        // Podcast matches
                        if (podcast.equalByUrl(entry.getValue().podcastUrl)) {
                            boolean stillInPodcast = false;

                            // Check whether the episode is still there
                            for (Episode episode : podcast.getEpisodes())
                                if (episode.equalByUrl(entry.getKey()))
                                    stillInPodcast = true;

                            // If it is not there and the episode metadata does
                            // not have any other information, delete the
                            // metadata
                            if (!stillInPodcast && entry.getValue().hasOnlyStateData()) {
                                // This is actually enough since the task
                                // storing the metadata will clean empty records
                                entry.getValue().isOld = null;
                                entry.getValue().resumeAt = null;
                            }
                        }
                    }
                }
            }.start();
        }
        // The other interesting case is when the load counter allowed the
        // clean-up to run but the podcast is already clean: Do nothing.
        // In all other cases: increment counter.
        else if (podcast != null && podcastLoadCounter % 10 != 0)
            podcastLoadCounter++;
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // pass
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast, PodcastLoadError code) {
        // pass
    }
}
