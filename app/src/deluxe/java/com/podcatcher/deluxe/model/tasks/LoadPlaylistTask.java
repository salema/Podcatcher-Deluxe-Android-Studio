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

package com.podcatcher.deluxe.model.tasks;

import com.podcatcher.deluxe.listeners.OnLoadPlaylistListener;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

/**
 * Get the playlist from the episode manager.
 */
public class LoadPlaylistTask extends AsyncTask<Void, Void, List<Episode>> {

    /**
     * Call back
     */
    private final WeakReference<OnLoadPlaylistListener> listener;

    /**
     * Filter
     */
    private final Podcast podcast;

    /**
     * Create new task.
     *
     * @param listener Callback to be alerted on completion. The listener is
     *                 held as a weak reference, so you can safely call this from an
     *                 activity without leaking it.
     * @param podcast  Podcast to use as a filter. The returned playlist will
     *                 only contain episodes that belong to the given podcast.
     *                 Setting this to <code>null</code> disables the filter and all
     *                 episodes in the playlist are returned.
     */
    public LoadPlaylistTask(OnLoadPlaylistListener listener, Podcast podcast) {
        this.listener = new WeakReference<>(listener);
        this.podcast = podcast;
    }

    @Override
    protected List<Episode> doInBackground(Void... nothing) {
        try {
            // 0. Block if episode metadata not yet available
            EpisodeManager.getInstance().blockUntilEpisodeMetadataIsLoaded();

            // 1. Get the playlist
            final List<Episode> playlist = EpisodeManager.getInstance().getPlaylist();

            // 2. Filter the playlist if podcast is set
            if (podcast != null && !playlist.isEmpty()) {
                final Iterator<Episode> episodes = playlist.iterator();

                while (episodes.hasNext()) {
                    final Episode current = episodes.next();

                    if (!podcast.equals(current.getPodcast()))
                        episodes.remove();
                }
            }

            // 3. Return the result
            return playlist;
        } catch (Exception e) {
            cancel(true);
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<Episode> playlist) {
        // Playlist available
        final OnLoadPlaylistListener listener = this.listener.get();

        if (listener != null)
            listener.onPlaylistLoaded(playlist);
    }
}
