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

import com.podcatcher.deluxe.BaseActivity.ContentMode;
import com.podcatcher.deluxe.EpisodeActivity;
import com.podcatcher.deluxe.EpisodeListActivity;
import com.podcatcher.deluxe.PodcastActivity;
import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.listeners.DownloadTaskListener;
import com.podcatcher.deluxe.listeners.OnDownloadEpisodeListener;
import com.podcatcher.deluxe.listeners.OnLoadDownloadsListener;
import com.podcatcher.deluxe.model.tasks.LoadDownloadsTask;
import com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask;
import com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;
import com.podcatcher.deluxe.model.types.Podcast;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED;
import static android.app.DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * This class is the part of the episode manager stack that handles the download
 * and deletion of episodes. It uses the Android {@link DownloadManager} API to
 * carry out the downloads.
 *
 * @see EpisodeManager
 */
public abstract class EpisodeDownloadManager extends EpisodeInformationManager implements
        DownloadTaskListener {

    /**
     * An {@link java.util.concurrent.Executor} for downloading episodes in parallel.
     */
    public final ThreadPoolExecutor downloadEpisodeExecutor;

    /**
     * Characters not allowed in file names
     */
    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'#!,&";

    /**
     * Maximum file/folder name length
     */
    private static final int MAX_LENGTH = 80;

    /**
     * The current number of downloaded episodes we know of
     */
    protected int downloadsSize = -1;

    /**
     * The call-back set for the complete download listeners
     */
    private Set<OnDownloadEpisodeListener> downloadListeners = new HashSet<>();
    /**
     * The receiver we register for download selections
     */
    private BroadcastReceiver onDownloadClicked = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Only react if this actually is a download clicked event
            if (ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                // Get clicked ids
                final long[] downloadIds = intent
                        .getLongArrayExtra(EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);

                if (downloadIds != null && downloadIds.length > 0) {
                    // Get the download id that was clicked (first if multiple)
                    final long downloadId = downloadIds[0];
                    // Go do the actual work in the episode manager
                    if (downloadId >= 0)
                        processDownloadClicked(downloadId);
                }
            }
        }
    };

    /**
     * Init the download episode manager.
     *
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodeDownloadManager(Podcatcher app) {
        super(app);

        // Register as a receiver for downloads selections so we are alerted
        // when a download is clicked in the DownloadManager UI
        podcatcher.registerReceiver(onDownloadClicked,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        // Init the executor. This is used exclusively by the episode manager
        // and makes sure that other parts of the application do not have to wait
        // for lengthy episode downloads to finish before their async tasks
        // can run on the default executor. We put in some extra threads here, since
        // downloads are mainly slow I/O and not very CPU intensive.
        final int threadCount = Runtime.getRuntime().availableProcessors() * 2 + 1;
        this.downloadEpisodeExecutor = new ThreadPoolExecutor(threadCount, threadCount,
                1L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);

                    public Thread newThread(@NonNull Runnable r) {
                        return new Thread(r, "DownloadEpisodeTask #" + count.getAndIncrement());
                    }
                });
        this.downloadEpisodeExecutor.allowCoreThreadTimeOut(true);
    }

    /**
     * Clean up given string to be suitable as a file/directory name. This works
     * by removing all reserved chars.
     *
     * @param name The String to clean up (not <code>null</code>).
     * @return A cleaned string, might have zero length.
     */
    public static String sanitizeAsFilename(String name) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            // If max length is reached, wait for next whitespace and truncate
            if (i > MAX_LENGTH && name.charAt(i) == ' ')
                break;

            // Remove un-allowed chars
            if (RESERVED_CHARS.indexOf(name.charAt(i)) == -1)
                builder.append(name.charAt(i));
        }

        // Make sure filename does not end with a dot
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '.')
            builder.deleteCharAt(builder.length() - 1);

        return builder.toString();
    }

    /**
     * Get the relative file path for the episode entry given. This reflects
     * where it would have been or should be stored locally when downloaded.
     * Note that this is still relative to the download directory set. Uses
     * {@link #sanitizeAsFilename(String)}. None of the parameter inputs is
     * allowed to be <code>null</code> or empty.
     *
     * @param podcast    The episode's owning podcast's name
     * @param episode    The episode's name
     * @param episodeUrl The episode's URL (used to determine the file ending)
     * @return The path string as podcast/episode.ending without reserved
     * characters
     */
    public static String sanitizeAsFilePath(String podcast, String episode, String episodeUrl) {
        // Extract file ending
        String fileEnding = "";
        try {
            final String path = Uri.parse(episodeUrl).getLastPathSegment();
            final int endingIndex = path.lastIndexOf('.');

            if (endingIndex > 0 && (endingIndex + 1) < path.length())
                fileEnding = path.substring(endingIndex);
        } catch (NullPointerException nex) {
            // Leave out file ending if URL is invalid
        }

        // Create sanitized path <podcast>/<episode>.<ending>
        return sanitizeAsFilename(podcast) + File.separatorChar
                + sanitizeAsFilename(episode) + sanitizeAsFilename(fileEnding);
    }

    /**
     * Initiate a download for the given episode. Will do nothing if the episode
     * is already downloaded or is currently downloading.
     *
     * @param episode Episode to get.
     */
    public void download(Episode episode) {
        if (episode != null && metadata != null && !isDownloadingOrDownloaded(episode)) {
            // Find or create the metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta == null) {
                meta = new EpisodeMetadata();
                metadata.put(episode.getMediaUrl(), meta);
            }

            // We need to put a download id. If the episode is already
            // downloaded (i.e. the file exists) and we somehow missed to catch
            // it, zero will work just fine.
            meta.downloadId = 0l;
            // Prepare metadata record
            meta.downloadProgress = -1;
            putAdditionalEpisodeInformation(episode, meta);

            // Mark metadata record as dirty
            metadataChanged = true;

            // Start the actual download
            try {
                new DownloadEpisodeTask(podcatcher, this)
                        .executeOnExecutor(downloadEpisodeExecutor, episode);
            } catch (RejectedExecutionException ree) {
                // Too many tasks running
                onEpisodeDownloadFailed(episode, EpisodeDownloadError.UNKNOWN);
            }
        }
    }

    @Override
    public void onEpisodeEnqueued(Episode episode, long id) {
        // Find the metadata record for the episode
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        if (meta != null) {
            meta.downloadId = id;

            // Mark metadata record as dirty
            metadataChanged = true;
        }
    }

    @Override
    public void onEpisodeDownloadProgressed(Episode episode, int percent) {
        // Find the metadata record for the episode
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        if (meta != null) {
            meta.downloadProgress = percent;
        }

        for (OnDownloadEpisodeListener listener : downloadListeners)
            listener.onDownloadProgress(episode, percent);
    }

    @Override
    public void onEpisodeDownloaded(Episode episode, File episodeFile) {
        // Find the metadata record for the episode
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        if (meta != null) {
            meta.filePath = episodeFile.getAbsolutePath();
            updateMediaFileSize(episode, episodeFile.length());

            for (OnDownloadEpisodeListener listener : downloadListeners)
                listener.onDownloadSuccess(episode);

            // Update counter
            if (downloadsSize != -1)
                downloadsSize++;

            // Mark metadata record as dirty
            metadataChanged = true;
        }
    }

    @Override
    public void onEpisodeDownloadFailed(Episode episode, EpisodeDownloadError error) {
        // Find the metadata record for the episode
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        if (meta != null) {
            meta.downloadId = null;
            meta.filePath = null;

            for (OnDownloadEpisodeListener listener : downloadListeners)
                listener.onDownloadFailed(episode, error);

            // Mark metadata record as dirty
            metadataChanged = true;
        }
    }

    /**
     * Cancel the download for given episode and delete all downloaded content.
     *
     * @param episode Episode to delete download for.
     */
    public void deleteDownload(Episode episode) {
        if (episode != null && metadata != null && isDownloadingOrDownloaded(episode)) {
            // Find the metadata information holder
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null) {
                // Keep info for the thread to run on
                final long downloadId = meta.downloadId;
                final String filePath = meta.filePath;
                // Go async when accessing download manager
                new Thread() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
                        // This should delete the download and remove all
                        // information from the download manager
                        try {
                            ((DownloadManager) podcatcher
                                    .getSystemService(Context.DOWNLOAD_SERVICE))
                                    .remove(downloadId);
                        } catch (IllegalArgumentException e) {
                            // There seem to be weird cases where this fails
                        }

                        // Make sure the file is deleted since this might not
                        // have taken care of by DownloadManager.remove() above
                        if (filePath != null)
                            // noinspection ResultOfMethodCallIgnored
                            new File(filePath).delete();
                    }
                }.start();

                meta.downloadId = null;
                meta.filePath = null;

                // Alert listeners
                for (OnDownloadEpisodeListener listener : downloadListeners)
                    listener.onDownloadDeleted(episode);

                // Mark metadata record as dirty
                metadataChanged = true;
                // Decrement counter
                if (downloadsSize != -1)
                    downloadsSize--;
            }
        }
    }

    /**
     * Check whether given episode is already downloaded and available on the
     * filesystem.
     *
     * @param episode Episode to check for.
     * @return <code>true</code> if the episode is downloaded and available.
     */
    public boolean isDownloaded(Episode episode) {
        return episode != null && metadata != null && isDownloaded(metadata.get(episode.getMediaUrl()));
    }

    /**
     * Check whether the download for this episode resides on a removable SD card.
     *
     * @param episode Episode to check for.
     * @return <code>true</code> if the local media file is on a removable storage. This only
     * works on Android >= 5.0, this method will return <code>false</code> on all earlier versions
     * of the platform even if the file is actually stored on a SD card.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean isDownloadedToSdCard(Episode episode) {
        try {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    isDownloaded(episode) && Environment.isExternalStorageRemovable(
                    new File(metadata.get(episode.getMediaUrl()).filePath));
        } catch (IllegalArgumentException iae) {
            // path not found/invalid
            return false;
        }
    }

    /**
     * Check whether given episode is currently downloading.
     *
     * @param episode Episode to check for.
     * @return <code>true</code> if the episode is currently in the process of
     * being downloaded.
     */
    public boolean isDownloading(Episode episode) {
        if (episode != null && metadata != null) {
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            return meta != null
                    && meta.downloadId != null
                    && meta.filePath == null;
        } else
            return false;
    }

    /**
     * Shortcut to check whether there is any download action going on with this
     * episode.
     *
     * @param episode Episode to check for.
     * @return <code>true</code> iff the episode is downloading or already
     * downloaded.
     */
    public boolean isDownloadingOrDownloaded(Episode episode) {
        return isDownloading(episode) || isDownloaded(episode);
    }

    /**
     * Get the download progress for this episode. Only returns a meaningful
     * value if the episode is currently pull from the net and the download task
     * told this manager about its progress.
     *
     * @param episode The Episode to look for.
     * @return The amount of data (in percent [0-100]) downloaded iff this
     * information is available or -1 in all other cases.
     */
    public int getDownloadProgress(Episode episode) {
        if (isDownloading(episode)) {
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            if (meta != null) {
                return meta.downloadProgress;
            } else
                return -1;
        } else
            return -1;
    }

    /**
     * Get the list of downloaded episodes. Returns only episodes fully
     * available locally. The episodes are sorted by date, latest first. Only
     * call this if you are sure the metadata is already available, if in doubt
     * use {@link LoadDownloadsTask}.
     *
     * @return The list of downloaded episodes (might be empty, but not
     * <code>null</code>).
     * @see LoadDownloadsTask
     * @see #getDownloadsAsync(OnLoadDownloadsListener)
     * @see OnLoadDownloadsListener
     */
    public List<Episode> getDownloads() {
        // Create empty result list
        List<Episode> result = new ArrayList<>();

        // This is only possible if the metadata is available
        if (metadata != null) {
            // Find downloads from metadata
            for (Entry<String, EpisodeMetadata> entry : metadata.entrySet()) {
                // Find records for downloaded episodes
                if (isDownloaded(entry.getValue())) {
                    // Create and add the downloaded episode
                    Episode download = entry.getValue().marshalEpisode(entry.getKey());

                    if (download != null)
                        result.add(download);
                }
            }

            // Since we have the downloads list here, we could just as well set
            // this and make the other methods return faster
            this.downloadsSize = result.size();
        }

        // Sort and return the list
        Collections.sort(result);
        return result;
    }

    /**
     * Get the list of downloaded episodes asynchronously.
     *
     * @param listener The listener to alert once the downloads are available.
     * @see #getDownloads()
     */
    public void getDownloadsAsync(OnLoadDownloadsListener listener) {
        getDownloadsAsync(listener, null);
    }

    /**
     * Get the list of downloaded episodes asynchronously and for the given
     * podcast only.
     *
     * @param listener The listener to alert once the downloads are available.
     * @param podcast  The podcast to filter for.
     * @see #getDownloads()
     */
    public void getDownloadsAsync(OnLoadDownloadsListener listener, Podcast podcast) {
        try {
            new LoadDownloadsTask(listener, podcast)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        } catch (RejectedExecutionException ree) {
            // TODO find a better solution here
            listener.onDownloadsLoaded(new ArrayList<Episode>());
        }
    }

    /**
     * @return The number of downloaded episodes.
     */
    public int getDownloadsSize() {
        if (downloadsSize == -1 && metadata != null)
            initDownloadsCounter();

        return downloadsSize == -1 ? 0 : downloadsSize;
    }

    /**
     * Get the absolute, local path to a downloaded episode.
     *
     * @param episode Episode to get local path for.
     * @return The complete local path to the downloaded episode or
     * <code>null</code> if the episode is not available locally.
     * @see #isDownloaded(Episode)
     */
    public String getLocalPath(Episode episode) {
        if (episode != null && metadata != null) {
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            return meta == null ? null : meta.filePath;
        } else
            return null;
    }

    /**
     * Add a download listener.
     *
     * @param listener Listener to add.
     * @see OnDownloadEpisodeListener
     */
    public void addDownloadListener(OnDownloadEpisodeListener listener) {
        downloadListeners.add(listener);
    }

    /**
     * Remove a download listener.
     *
     * @param listener Listener to remove.
     * @see OnDownloadEpisodeListener
     */
    public void removeDownloadListener(OnDownloadEpisodeListener listener) {
        downloadListeners.remove(listener);
    }

    /**
     * @return The default podcast episode download folder.
     */
    public static File getDefaultDownloadFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
    }

    private void initDownloadsCounter() {
        this.downloadsSize = 0;

        for (EpisodeMetadata meta : metadata.values())
            if (isDownloaded(meta))
                downloadsSize++;
    }

    private boolean isDownloaded(EpisodeMetadata meta) {
        return meta != null
                && meta.downloadId != null
                && meta.filePath != null
                && new File(meta.filePath).exists();
    }

    private void processDownloadClicked(long downloadId) {
        // Nothing we can do if the meta data is not available
        if (metadata != null) {
            // Find download from metadata
            for (Entry<String, EpisodeMetadata> entry : metadata.entrySet()) {
                final EpisodeMetadata data = entry.getValue();

                // Only act if we care for this download
                if (data.downloadId != null && data.downloadId == downloadId) {
                    // Create the downloading episode
                    Episode download = entry.getValue().marshalEpisode(entry.getKey());
                    if (download != null) {
                        Intent intent = new Intent(podcatcher.getApplicationContext(),
                                PodcastActivity.class)
                                .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                                .putExtra(EpisodeListActivity.PODCAST_URL_KEY, download.getPodcast().getUrl())
                                .putExtra(EpisodeActivity.EPISODE_URL_KEY, download.getMediaUrl())
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                        // Make the app switch to it.
                        podcatcher.startActivity(intent);
                    }
                }
            }
        }
    }
}
