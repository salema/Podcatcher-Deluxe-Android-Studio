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

package com.podcatcher.deluxe.model.tasks.remote;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.SettingsActivity;
import com.podcatcher.deluxe.listeners.DownloadTaskListener;
import com.podcatcher.deluxe.model.EpisodeDownloadManager;
import com.podcatcher.deluxe.model.types.Episode;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_LOCAL_FILENAME;
import static android.app.DownloadManager.COLUMN_REASON;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;
import static android.app.DownloadManager.ERROR_FILE_ALREADY_EXISTS;
import static android.app.DownloadManager.ERROR_INSUFFICIENT_SPACE;
import static android.app.DownloadManager.Request.NETWORK_MOBILE;
import static android.app.DownloadManager.Request.NETWORK_WIFI;
import static android.app.DownloadManager.STATUS_FAILED;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;
import static com.podcatcher.deluxe.Podcatcher.AUTHORIZATION_KEY;
import static com.podcatcher.deluxe.Podcatcher.USER_AGENT_KEY;
import static com.podcatcher.deluxe.Podcatcher.userAgentValue;
import static com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError.BAD_EPISODE;
import static com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError.DESTINATION_NOT_WRITABLE;
import static com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError.DOWNLOAD_APP_DISABLED;
import static com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError.NO_SPACE;
import static com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError.UNKNOWN;

/**
 * Async task that triggers the download of an episode. The task will be alive
 * and busy in its doInBackground() method as long as the download takes. It
 * will publish updates of the download's progress to the call-back attached.
 * Use a new task for each episode you want to download. Make sure not to give
 * <code>null</code> as an episode to the {@link #doInBackground(Episode...)}
 * method or things will break.
 */
public class DownloadEpisodeTask extends AsyncTask<Episode, Long, Void> {

    /**
     * The podcatcher app handle
     */
    private Podcatcher podcatcher;
    /**
     * The listener (episode manager) we report to
     */
    private DownloadTaskListener listener;
    /**
     * The system download manager
     */
    private DownloadManager downloadManager;

    /**
     * The episode we are downloading
     */
    private Episode episode;
    /**
     * The file the episode is downloaded to
     */
    private File episodeFile;
    /**
     * The current percentage state of the download [0...100]
     */
    private int percentProgress;
    /**
     * Flag on whether the download should only occur on wifi and
     * be cancelled if the devices switches to mobile (metered) data.
     */
    private final boolean wifiOnly;
    /**
     * Flag on whether the download needs moving after the fact
     */
    private boolean needsPostDownloadMove = false;

    /**
     * The episode download error code
     */
    private EpisodeDownloadError downloadError = UNKNOWN;

    /**
     * Episode download error codes as returned by
     * {@link DownloadTaskListener#onEpisodeDownloadFailed(Episode, EpisodeDownloadError)}
     * .
     */
    public enum EpisodeDownloadError {
        /**
         * An error occurred, but the reason is unknown and/or does not fit any
         * of the other codes.
         */
        UNKNOWN,

        /**
         * There is not enough storage on the device.
         */
        NO_SPACE,

        /**
         * The desired destination for the episode's media file cannot be
         * written to.
         */
        DESTINATION_NOT_WRITABLE,

        /**
         * The device's download app is disabled.
         */
        DOWNLOAD_APP_DISABLED,

        /**
         * The episode does not provide a valid media URL
         */
        BAD_EPISODE
    }

    /**
     * Create a new task.
     * @param podcatcher The podcatcher app handle (not <code>null</code>).
     * @param listener   The call-back used by the task (not <code>null</code>).
     * @param wifiOnly   Whether the download should only occur on wifi.
     */
    public DownloadEpisodeTask(Podcatcher podcatcher, DownloadTaskListener listener, boolean wifiOnly) {
        this.podcatcher = podcatcher;
        this.listener = listener;
        this.wifiOnly = wifiOnly;

        // Get handle to the system download manager which does all the
        // downloading for us
        downloadManager = (DownloadManager)
                podcatcher.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    protected Void doInBackground(Episode... params) {
        this.episode = params[0];

        // Update the thread name to include the episode working on
        if (BuildConfig.DEBUG && episode != null)
            Thread.currentThread().setName(Thread.currentThread().getName() +
                    " [" + episode.getName() + "]");

        // Find the podcast directory and the path to store episode under
        final File podcastDir = new File(PreferenceManager.getDefaultSharedPreferences(podcatcher)
                .getString(SettingsActivity.KEY_DOWNLOAD_FOLDER,
                        EpisodeDownloadManager.getDefaultDownloadFolder().getAbsolutePath()));
        final String subPath = EpisodeDownloadManager.sanitizeAsFilePath(
                episode.getPodcast().getName(), episode.getName(), episode.getMediaUrl());
        // The actual episode file
        final File localFile = new File(podcastDir, subPath);

        // The episode is already there, alert listener
        if (localFile.exists()) {
            // Wait one round in order to give the "download started" animation
            // time to complete. Otherwise the UI will show artifacts because we
            // return too early (i.e. before the animation completed).
            try {
                TimeUnit.MILLISECONDS.sleep(podcatcher.getResources().getInteger(
                        android.R.integer.config_longAnimTime));
            } catch (InterruptedException e) {
                // pass
            }

            this.episodeFile = localFile;
        }
        // Start download because the episode is not there
        else {
            // Make sure podcast directory exists
            // noinspection ResultOfMethodCallIgnored
            localFile.getParentFile().mkdirs();

            // Create the request
            Request download;
            try {
                download = new Request(Uri.parse(episode.getMediaUrl()))
                        .setDestinationUri(Uri.fromFile(localFile))
                        .setTitle(episode.getName())
                        .setDescription(episode.getPodcast().getName())
                        .setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                        .addRequestHeader(USER_AGENT_KEY, userAgentValue)
                        .addRequestHeader("Cache-Control", "no-store")
                        .setAllowedNetworkTypes(NETWORK_WIFI | (wifiOnly ? 0 : NETWORK_MOBILE))
                        .setAllowedOverRoaming(!wifiOnly);

                download.allowScanningByMediaScanner();
            } catch (NullPointerException | IllegalArgumentException re) {
                // The Android DownloadManager will reject URL that
                // don't start with http or https
                return cancelAndSetError(BAD_EPISODE);
            }

            // Set auth if available
            final String auth = episode.getPodcast().getAuthorization();
            if (auth != null)
                download.addRequestHeader(AUTHORIZATION_KEY, auth);

            // Start the download
            long downloadId;
            try {
                downloadId = downloadManager.enqueue(download);
            } catch (SecurityException se) {
                // This happens if the download manager has not the rights
                // to write to the selected downloads directory
                needsPostDownloadMove = true;

                // Download the file to a temp folder and move it the the wanted
                // destination once the download completed
                download.setDestinationUri(Uri.fromFile(
                        new File(podcatcher.getExternalCacheDir(), localFile.getName())));
                try {
                    downloadId = downloadManager.enqueue(download);
                } catch (SecurityException se2) {
                    // This is weird and should not happen, but we have seen this on some devices
                    return cancelAndSetError(DESTINATION_NOT_WRITABLE);
                }
            } catch (IllegalArgumentException lae) {
                // The happens if the download app on the device is disabled
                return cancelAndSetError(DOWNLOAD_APP_DISABLED);
            } catch (NullPointerException npe) {
                // The download manager does not like our request because it is incomplete
                return cancelAndSetError(BAD_EPISODE);
            }

            // We need to tell our listener about the download id, to
            // separate it from percentage done, put minus sign. See
            // onProgressUpdate() below.
            publishProgress(downloadId > 0 ? downloadId * -1 : downloadId);

            // Start checking the download manager for status updates
            boolean finished = false;
            while (!isCancelled() && !finished) {
                // Wait between polls
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    // pass
                }

                // Find download information
                final Cursor info = downloadManager.query(new Query().setFilterById(downloadId));
                // There should be information on the download
                if (info != null && info.moveToFirst()) {
                    final int state = info.getInt(info.getColumnIndex(COLUMN_STATUS));
                    switch (state) {
                        case STATUS_SUCCESSFUL:
                            // This is the file the download manager got for us
                            final File downloadedFile = new File(info.getString(info
                                    .getColumnIndex(COLUMN_LOCAL_FILENAME)));

                            // It might need to be moved to its final position
                            if (needsPostDownloadMove) {
                                // This worked
                                if (moveFile(downloadedFile, localFile))
                                    this.episodeFile = localFile;
                                    // Move operation failed -> download failed
                                else {
                                    this.downloadError = DESTINATION_NOT_WRITABLE;
                                    cancel(false);
                                }

                                // We remove the file from the system's download
                                // manager here, since we moved the downloaded
                                // file (or it failed anyway)
                                downloadManager.remove(downloadId);
                            } else
                                this.episodeFile = downloadedFile;

                            finished = true;
                            break;
                        case STATUS_FAILED:
                            final int reason = info.getInt(info.getColumnIndex(COLUMN_REASON));
                            switch (reason) {
                                case ERROR_FILE_ALREADY_EXISTS:
                                    // This case is actually fine, finish
                                    finished = true;
                                    break;
                                case ERROR_INSUFFICIENT_SPACE:
                                    this.downloadError = NO_SPACE;
                                    // Fall through here, code below should run
                                default:
                                    downloadManager.remove(downloadId);

                                    cancel(false);
                                    break;
                            }

                            break;
                        default:
                            // Update progress
                            final long total = info.getLong(info
                                    .getColumnIndex(COLUMN_TOTAL_SIZE_BYTES));
                            final long progress = info.getLong(info
                                    .getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));

                            if (total > 0 && progress > 0 && total >= progress)
                                publishProgress((long) (((float) progress / (float) total) * 100));
                    }
                }

                // Close cursor
                if (info != null)
                    info.close();
            }
        }

        revertThreadName();
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        long progress = values[0];

        // This is the download id (because it is < 0, see above)
        if (progress < 0)
            listener.onEpisodeEnqueued(episode, progress * -1);
            // This is the percentage of download done
        else if (progress > 0 && progress != percentProgress) {
            percentProgress = (int) progress;
            listener.onEpisodeDownloadProgressed(episode, percentProgress);
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        // If the episodeFile member is set, the download was successful
        if (episodeFile != null)
            listener.onEpisodeDownloaded(episode, episodeFile);
        else
            onCancelled(result);
    }

    @Override
    protected void onCancelled(Void result) {
        listener.onEpisodeDownloadFailed(episode, downloadError);
    }

    private boolean moveFile(File from, File to) {
        boolean success = true;

        BufferedInputStream reader = null;
        BufferedOutputStream writer = null;
        // Move file over, put try catch to set return value to false is the
        // move fails
        try {
            reader = new BufferedInputStream(new FileInputStream(from));
            writer = new BufferedOutputStream(new FileOutputStream(to));

            byte[] buffer = new byte[1024];
            while (reader.read(buffer) > 0)
                writer.write(buffer);

        } catch (IOException ioe) {
            success = false;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    // pass
                }
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    // pass
                }

            // noinspection ResultOfMethodCallIgnored
            from.delete();
        }

        return success;
    }

    /**
     * Set the {@link EpisodeDownloadError} and cancels the task.
     *
     * @param error The error that occurred.
     * @return Always null so you can forward it.
     */
    private Void cancelAndSetError(EpisodeDownloadError error) {
        this.downloadError = error;
        cancel(false);
        revertThreadName();

        return null;
    }

    private void revertThreadName() {
        if (BuildConfig.DEBUG)
            Thread.currentThread().setName(Thread.currentThread().getName().split(" \\[")[0]);
    }
}
