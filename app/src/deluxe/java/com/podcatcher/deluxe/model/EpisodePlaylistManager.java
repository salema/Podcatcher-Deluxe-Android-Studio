/**
 * Copyright 2012-2016 Kevin Hausmann
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
import com.podcatcher.deluxe.listeners.OnChangePlaylistListener;
import com.podcatcher.deluxe.listeners.OnLoadPlaylistListener;
import com.podcatcher.deluxe.model.tasks.LoadPlaylistTask;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;
import com.podcatcher.deluxe.model.types.Podcast;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.RejectedExecutionException;

/**
 * Episode manager in the episode manager stack that cares for the playlist.
 *
 * @see EpisodeManager
 */
public abstract class EpisodePlaylistManager extends EpisodeDownloadManager {

    /**
     * Helper to make playlist methods more efficient
     */
    private int playlistSize = -1;

    /**
     * The call-back set for the playlist listeners
     */
    private Set<OnChangePlaylistListener> playlistListeners = new HashSet<>();

    /**
     * Init the episode playlist manager.
     *
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodePlaylistManager(Podcatcher app) {
        super(app);
    }

    /**
     * @return The current playlist. Might be empty but not <code>null</code>.
     * Only call this if you are sure the metadata is already available,
     * if in doubt use {@link LoadPlaylistTask}.
     * @see LoadPlaylistTask
     * @see #getPlaylistAsync(OnLoadPlaylistListener)
     * @see OnLoadPlaylistListener
     */
    public List<Episode> getPlaylist() {
        // The resulting playlist
        TreeMap<Integer, Episode> playlist = new TreeMap<>();

        // This is only possible if the metadata is available
        if (metadata != null) {
            // Find playlist entries from metadata
            for (Entry<String, EpisodeMetadata> entry : metadata.entrySet()) {
                // Find records for playlist entries
                if (entry.getValue().playlistPosition != null) {
                    // Create and add the downloaded episode
                    Episode playlistEntry = entry.getValue().marshalEpisode(entry.getKey());
                    playlist.put(entry.getValue().playlistPosition, playlistEntry);
                }
            }

            // Since we have the playlist here, we could just as well set this
            // and make the other methods return faster
            this.playlistSize = playlist.size();
        }

        return new ArrayList<>(playlist.values());
    }

    /**
     * Get the list of enqueued episodes asynchronously.
     *
     * @param listener The listener to alert once the playlist is available.
     * @see #getPlaylist()
     */
    public void getPlaylistAsync(OnLoadPlaylistListener listener) {
        getPlaylistAsync(listener, null);
    }

    /**
     * Get the list of enqueued episodes asynchronously and for the given
     * podcast only.
     *
     * @param listener The listener to alert once the playlist is available.
     * @param podcast  The podcast to filter for.
     * @see #getPlaylist()
     */
    public void getPlaylistAsync(OnLoadPlaylistListener listener, Podcast podcast) {
        try {
            new LoadPlaylistTask(listener, podcast)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        } catch (RejectedExecutionException ree) {
            // TODO Find better solution here
            listener.onPlaylistLoaded(new ArrayList<Episode>());
        }
    }

    /**
     * @return The current playlist filtered to only contain episodes available
     * locally, keyed using their original playlist position. Might be
     * empty but not <code>null</code>. Only call this if you are sure
     * the metadata is already available.
     * @see EpisodePlaylistManager#getPlaylist()
     */
    public SortedMap<Integer, Episode> getDownloadedPlaylist() {
        final List<Episode> playlist = getPlaylist();
        final SortedMap<Integer, Episode> filteredPlaylist = new TreeMap<>();

        for (int index = 0; index < playlist.size(); index++) {
            final Episode episode = playlist.get(index);

            if (isDownloaded(episode))
                filteredPlaylist.put(index, episode);
        }

        return filteredPlaylist;
    }

    /**
     * @return The number of episodes in the playlist.
     */
    public int getPlaylistSize() {
        if (playlistSize == -1 && metadata != null)
            initPlaylistCounter();

        return playlistSize == -1 ? 0 : playlistSize;
    }

    /**
     * @return Whether the current playlist has any entries.
     */
    public boolean isPlaylistEmpty() {
        return getPlaylistSize() <= 0;
    }

    /**
     * Find out whether some episode is alone in the playlist.
     *
     * @param episode Episode to exclude from empty check.
     * @return Whether the current playlist has any entries besides the episode
     * given. Will also return <code>true</code> if the playlist has no
     * entries at all.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPlaylistEmptyBesides(Episode episode) {
        return isPlaylistEmpty() || (getPlaylistSize() == 1 && isInPlaylist(episode));
    }

    /**
     * Check whether a specific episode already exists in the playlist.
     *
     * @param episode Episode to check for.
     * @return <code>true</code> iff present in playlist.
     */
    public boolean isInPlaylist(Episode episode) {
        return getPlaylistPosition(episode) != -1;
    }

    /**
     * Find the position of the given episode in the playlist.
     *
     * @param episode Episode to find.
     * @return The position of the episode (staring at 0) or -1 if not present.
     */
    public int getPlaylistPosition(Episode episode) {
        int result = -1;

        if (episode != null && metadata != null) {
            // Find metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null && meta.playlistPosition != null)
                result = meta.playlistPosition;
        }

        return result;
    }

    /**
     * Add an episode to the playlist. The episode will be appended to the end
     * of the list. Does nothing if the episode is already in the playlist.
     *
     * @param episode The episode to add.
     */
    public void appendToPlaylist(Episode episode) {
        insertAtPlaylistPosition(episode, getPlaylistSize());
    }

    /**
     * Insert an episode at the given playlist position. The episode will be
     * appended if the given position is greater than the number of items in the
     * playlist. Does nothing if the episode is already in the playlist.
     *
     * @param episode  Episode to insert.
     * @param position Index to insert at (starting at 0).
     */
    public void insertAtPlaylistPosition(Episode episode, int position) {
        if (episode != null && metadata != null && position >= 0) {
            // Only insert the episode if it is not already part of the playlist
            if (!isInPlaylist(episode)) {
                // Find or create the metadata information holder
                EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
                if (meta == null) {
                    meta = new EpisodeMetadata();
                    metadata.put(episode.getMediaUrl(), meta);
                }

                // Increment all other positions if needed
                if (position < getPlaylistSize())
                    for (EpisodeMetadata otherMeta : metadata.values())
                        if (otherMeta.playlistPosition != null &&
                                otherMeta.playlistPosition >= position)
                            otherMeta.playlistPosition++;

                // Put metadata information
                meta.playlistPosition = position < getPlaylistSize() ? position : getPlaylistSize();
                putAdditionalEpisodeInformation(episode, meta);

                // Increment counter
                if (playlistSize != -1)
                    playlistSize++;

                // Alert listeners
                for (OnChangePlaylistListener listener : playlistListeners)
                    listener.onPlaylistChanged();

                // Mark metadata record as dirty
                metadataChanged = true;
            }
        }
    }

    /**
     * Delete given episode off the playlist.
     *
     * @param episode Episode to pop.
     */
    public void removeFromPlaylist(Episode episode) {
        if (episode != null && metadata != null) {
            // Find the metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null && meta.playlistPosition != null) {
                // Update the playlist positions for all entries beyond the one we are removing
                for (Entry<String, EpisodeMetadata> stringEpisodeMetadataEntry : metadata.entrySet()) {
                    EpisodeMetadata other = stringEpisodeMetadataEntry.getValue();

                    // Find records for playlist entries
                    if (other.playlistPosition != null &&
                            other.playlistPosition > meta.playlistPosition)
                        other.playlistPosition--;
                }

                // Reset the playlist position for given episode
                meta.playlistPosition = null;

                // Decrement counter
                if (playlistSize != -1)
                    playlistSize--;

                // Alert listeners
                for (OnChangePlaylistListener listener : playlistListeners)
                    listener.onPlaylistChanged();

                // Mark metadata record as dirty
                metadataChanged = true;
            }
        }
    }

    /**
     * Add a playlist listener.
     *
     * @param listener Listener to add.
     * @see OnChangePlaylistListener
     */
    public void addPlaylistListener(OnChangePlaylistListener listener) {
        playlistListeners.add(listener);
    }

    /**
     * Remove a playlist listener.
     *
     * @param listener Listener to remove.
     * @see OnChangePlaylistListener
     */
    public void removePlaylistListener(OnChangePlaylistListener listener) {
        playlistListeners.remove(listener);
    }

    private void initPlaylistCounter() {
        this.playlistSize = 0;

        for (EpisodeMetadata meta : metadata.values())
            if (meta.playlistPosition != null)
                playlistSize++;
    }
}
