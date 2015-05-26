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

import com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError;
import com.podcatcher.deluxe.model.types.Episode;

/**
 * Interface for the controller to implement when the user requests an episode
 * to be downloaded locally.
 */
public interface OnDownloadEpisodeListener {

    /**
     * Start/stop the download for the current episode.
     */
    void onToggleDownload();

    /**
     * Called on the listener to alert it about a download progress update.
     *
     * @param episode The episode the progress was made for.
     * @param percent The percentage of episode downloaded [0..100].
     */
    void onDownloadProgress(Episode episode, int percent);

    /**
     * Called on the listener once a download finished successfully.
     *
     * @param episode The episode now available offline.
     */
    void onDownloadSuccess(Episode episode);

    /**
     * Called on the listener if a download failed.
     *
     * @param episode The episode that failed to download.
     * @param error   The reason as in {@link EpisodeDownloadError}.
     */
    void onDownloadFailed(Episode episode, EpisodeDownloadError error);

    /**
     * Called on the listener if a download is removed.
     *
     * @param episode The episode the local copy was delete of.
     */
    void onDownloadDeleted(Episode episode);
}
