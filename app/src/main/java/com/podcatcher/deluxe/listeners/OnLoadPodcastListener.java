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

package com.podcatcher.deluxe.listeners;

import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

/**
 * Interface definition for a callback to be invoked when a podcast is loaded.
 * Also provides some means to monitor the load progress.
 */
public interface OnLoadPodcastListener {

    /**
     * Called on progress update.
     *
     * @param podcast  Podcast loading.
     * @param progress Percent of podcast RSS file loaded or flag from
     *                 {@link Progress}. Note that this only works if the http
     *                 connection reports its content length correctly. Otherwise
     *                 (and this happens in the wild out there) percent might be
     *                 >100.
     */
    void onPodcastLoadProgress(Podcast podcast, Progress progress);

    /**
     * Called when the podcast has an "old" URL that should be replaced. This call completes
     * loading from that URL, {@link #onPodcastLoaded(Podcast)} will <em>not</em> be called
     * in addition. Listeners should use this call-back to update their internal state and can
     * rely on the {@link com.podcatcher.deluxe.model.PodcastManager} to actually load the
     * podcast from its new online location <em>after</em> calling this method, resulting in a
     * call of {@link #onPodcastLoaded(Podcast)} for the new URL.
     * <br/>
     * This will <em>never</em> be called on a listener if sync is active.
     * Instead, loading will continue as normal and either {@link #onPodcastLoaded(Podcast)}
     * or {@link #onPodcastLoadFailed(Podcast, PodcastLoadError)} will be called.
     *
     * @param podcast Podcast not loaded.
     * @param newUrl  The new URL to switch to.
     * @see SyncManager#getActiveControllerCount()
     */
    void onPodcastMoved(Podcast podcast, String newUrl);

    /**
     * Called on completion.
     *
     * @param podcast Podcast loaded.
     */
    void onPodcastLoaded(Podcast podcast);

    /**
     * Called when loading the podcast failed.
     *
     * @param podcast Podcast failing to load.
     * @param code    The reason for the failure.
     */
    void onPodcastLoadFailed(Podcast podcast, PodcastLoadError code);
}
