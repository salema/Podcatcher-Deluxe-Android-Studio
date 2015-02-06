/** Copyright 2012-2015 Kevin Hausmann
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
import com.podcatcher.deluxe.listeners.OnEpisodeInformationChangedListener;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is the part of the episode manager stack that handles additional
 * information such as episode duration or download file size.
 *
 * @see com.podcatcher.deluxe.model.EpisodeManager
 */
public abstract class EpisodeInformationManager extends EpisodeBaseManager {

    /**
     * The call-back set for the episode information listeners
     */
    private Set<OnEpisodeInformationChangedListener> changedListeners = new HashSet<>();

    /**
     * Init the episode information manager.
     *
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodeInformationManager(Podcatcher app) {
        super(app);
    }

    /**
     * Find duration information for given episode.
     *
     * @param episode Episode to look for.
     * @return The duration in seconds or -1 if no data is found.
     */
    public int findDuration(Episode episode) {
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        return meta == null ? -1 : meta.episodeDuration == null ? -1 : meta.episodeDuration;
    }

    /**
     * Set the duration information.
     *
     * @param episode     Episode to set duration for.
     * @param newDuration The new duration in seconds, the method will not do
     *                    anything unless the value given is greater than zero.
     */
    public void updateDuration(Episode episode, int newDuration) {
        if (newDuration > 0) {
            // Update metadata record if the duration is new or different
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null &&
                    (meta.episodeDuration == null || meta.episodeDuration != newDuration)) {
                meta.episodeDuration = newDuration;

                // Mark metadata record as dirty
                metadataChanged = true;
            }

            episode.setDuration(newDuration);

            // Alert listeners
            for (OnEpisodeInformationChangedListener listener : changedListeners)
                listener.onDurationChanged(episode);
        }
    }

    /**
     * Find file size information for given episode.
     *
     * @param episode Episode to look for.
     * @return The media file size in bytes or -1 if no data is found.
     */
    public long findMediaFileSize(Episode episode) {
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        return meta == null ? -1 : meta.episodeFileSize == null ? -1 : meta.episodeFileSize;
    }

    /**
     * Set the media file size information.
     *
     * @param episode Episode to set duration for.
     * @param newSize The new file size, the method will not do anything unless
     *                the value given is greater than zero
     */
    public void updateMediaFileSize(Episode episode, long newSize) {
        if (newSize > 0) {
            // Update metadata record if the size is new or different
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null &&
                    (meta.episodeFileSize == null || meta.episodeFileSize != newSize)) {
                meta.episodeFileSize = newSize;

                // Mark metadata record as dirty
                metadataChanged = true;
            }

            episode.setFileSize(newSize);

            // Alert listeners
            for (OnEpisodeInformationChangedListener listener : changedListeners)
                listener.onMediaFileSizeChanged(episode);
        }
    }

    /**
     * Add an information changed listener.
     *
     * @param listener Listener to add.
     * @see OnEpisodeInformationChangedListener
     */
    public void addInformationChangedListener(OnEpisodeInformationChangedListener listener) {
        changedListeners.add(listener);
    }

    /**
     * Remove an information changed listener.
     *
     * @param listener Listener to remove.
     * @see OnEpisodeInformationChangedListener
     */
    public void removeInformationChangedListener(OnEpisodeInformationChangedListener listener) {
        changedListeners.remove(listener);
    }
}
