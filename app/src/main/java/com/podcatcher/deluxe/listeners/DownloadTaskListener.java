/** Copyright 2012-2014 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */
package com.podcatcher.deluxe.listeners;

import com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask;
import com.podcatcher.deluxe.model.types.Episode;

import java.io.File;

/**
 * Interface definition for a callback to be invoked when an episode is downloaded.
 * Also provides some means to monitor the download progress.
 */
public interface DownloadTaskListener {

    /**
     * Called on the listener when the episode is enqueued.
     *
     * @param episode The episode now downloading.
     * @param id      The download manager id for the download.
     */
    void onEpisodeEnqueued(Episode episode, long id);

    /**
     * Called on the listener when the progress of the download for the
     * episode advanced.
     *
     * @param episode The episode downloading.
     * @param percent The percent value currently downloaded [0...100].
     */
    void onEpisodeDownloadProgressed(Episode episode, int percent);

    /**
     * Called on the listener if the episode requested to be downloaded is
     * already available on the device's storage.
     *
     * @param episode     The episode the task was started for.
     * @param episodeFile The local file.
     */
    void onEpisodeDownloaded(Episode episode, File episodeFile);

    /**
     * Called on the listener when the download for the episode fails for
     * some reason.
     *
     * @param episode The episode the download failed for.
     * @param error   The reason for failure per {@link DownloadEpisodeTask.EpisodeDownloadError}
     */
    void onEpisodeDownloadFailed(Episode episode, DownloadEpisodeTask.EpisodeDownloadError error);
}
