/** Copyright 2012-2014 Kevin Hausmann
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
    public void onPodcastLoadProgress(Podcast podcast, Progress progress);

    /**
     * Called on completion.
     *
     * @param podcast Podcast loaded.
     */
    public void onPodcastLoaded(Podcast podcast);

    /**
     * Called when loading the podcast failed.
     *
     * @param podcast Podcast failing to load.
     * @param code    The reason for the failure.
     */
    public void onPodcastLoadFailed(Podcast podcast, PodcastLoadError code);
}
