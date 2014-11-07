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

package com.podcatcher.deluxe.model;

import android.os.Handler;

import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.listeners.OnLoadEpisodeMetadataListener;
import com.podcatcher.deluxe.listeners.OnStoreEpisodeMetadataListener;
import com.podcatcher.deluxe.model.tasks.StoreEpisodeMetadataTask;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base for the episode manager's class hierarchy. This sets things up by
 * defining the basic data structures.
 *
 * @see EpisodeManager
 */
public abstract class EpisodeBaseManager implements OnLoadEpisodeMetadataListener,
        OnStoreEpisodeMetadataListener {

    /**
     * The file name to store local episode metadata information under
     */
    public static final String METADATA_FILENAME = "episodes.xml";

    /**
     * The application itself (used e.g. as context in tasks)
     */
    protected Podcatcher podcatcher;

    /**
     * The metadata information held for episodes
     */
    protected Map<String, EpisodeMetadata> metadata;
    /**
     * Flag to indicate whether metadata is dirty
     */
    protected boolean metadataChanged;

    /**
     * Amount of milliseconds between {@link #saveState()} calls
     */
    private long PERSIST_METADATA_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    /**
     * Flag to indicate whether the store meta data task is active
     */
    private boolean isStoreTaskRunning = false;
    /**
     * Handler for periodic meta data dirty checks
     */
    private Handler persistMetaDataHandler = new Handler();
    /**
     * Latch we use to block all threads until we have our data
     */
    private CountDownLatch latch = new CountDownLatch(1);
    /**
     * The runnable that calls {@link #saveState()}
     */
    private Runnable persistMetaDataRunnable = new Runnable() {

        @Override
        public void run() {
            saveState();

            persistMetaDataHandler.postDelayed(persistMetaDataRunnable, PERSIST_METADATA_INTERVAL);
        }
    };

    /**
     * Init the base episode manager.
     *
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodeBaseManager(Podcatcher app) {
        // We use some of its method below, so we keep a reference to the
        // application object.
        this.podcatcher = app;

        // We regularly check for the episode meta data to be dirty and run the
        // task to persist it if needed. This is useful because the meta data is
        // not only changed via the UI, but also from the play service or
        // download tasks.
        persistMetaDataHandler.postDelayed(persistMetaDataRunnable, PERSIST_METADATA_INTERVAL);
    }

    @Override
    public void onEpisodeMetadataLoaded(Map<String, EpisodeMetadata> metadata) {
        // We want our metadata to be thread safe, since we might load some
        // clean-up work off to other threads.
        this.metadata = new ConcurrentHashMap<>(metadata);
        this.metadataChanged = false;

        // Here we need to release all threads (AsyncTasks) that might be
        // waiting for the episode metadata to become available
        latch.countDown();
    }

    /**
     * This blocks the calling thread until the episode metadata has become
     * available during the application's start-up. Once the metadata is read,
     * this method returns immediately.
     *
     * @throws InterruptedException When the thread is interrupted while
     *                              waiting.
     */
    public void blockUntilEpisodeMetadataIsLoaded() throws InterruptedException {
        latch.await();
    }

    /**
     * Persist the manager's data to disk. It is save to call this at any time,
     * if there is no change in the episode meta data, no action is taken.
     */
    @SuppressWarnings("unchecked")
    public void saveState() {
        // Run store task if it is not running and meta data is dirty
        if (metadataChanged && metadata != null && !isStoreTaskRunning) {
            // Make sure task does not run twice
            isStoreTaskRunning = true;

            // Store a copy of the actual map, since there might come in changes
            // to the meta data while the task is running and that would lead to
            // a concurrent modification exception
            new StoreEpisodeMetadataTask(podcatcher, this).execute(new HashMap<>(metadata));

            // Reset the flag, so the list will only be saved if changed again.
            metadataChanged = false;
        }
    }

    @Override
    public void onEpisodeMetadataStored() {
        isStoreTaskRunning = false;
    }

    @Override
    public void onEpisodeMetadataStoreFailed(Exception exception) {
        isStoreTaskRunning = false;
    }

    /**
     * Utility method to populate an episode's metadata object.
     *
     * @param episode Episode to take data from
     * @param meta    Metadata holder to populate
     */
    protected void putAdditionalEpisodeInformation(Episode episode, EpisodeMetadata meta) {
        // We need all there object to be present: episode, podcast and holder
        if (episode != null && meta != null && episode.getPodcast() != null) {
            meta.episodeName = episode.getName();
            meta.episodePubDate = episode.getPubDate();
            meta.episodeMediaType = episode.getMediaType();
            meta.episodeDescription = episode.getDescription();
            meta.podcastName = episode.getPodcast().getName();
            meta.podcastUrl = episode.getPodcast().getUrl();

            final int duration = episode.getDuration();
            meta.episodeDuration = duration > 0 ? duration : null;
            final long size = episode.getMediaSize();
            meta.episodeFileSize = size > 0 ? size : null;
        }
    }
}
