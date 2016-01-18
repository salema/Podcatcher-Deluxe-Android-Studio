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
package com.podcatcher.deluxe.model.sync;

import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

import android.os.AsyncTask;

import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Abstract async task base class for podcast sync controllers to use.
 * Defines some common helpers, in particular a {@link #onProgressUpdate(Map.Entry[])}
 * that acts on the app's model by actually loading, adding and removing podcasts. It also
 * offers a semaphore that allows sub-classes to wait for all that action to finish.
 */
public abstract class SyncPodcastListTask extends AsyncTask<Void, Map.Entry<Boolean, Podcast>, Void> {

    /**
     * Podcast manager handle.
     */
    private final PodcastManager podcastManager = PodcastManager.getInstance();

    /**
     * This task triggers some LoadPodcastTasks and might need to track their progress.
     */
    protected final Semaphore allPodcastLoadsFinishedSemaphore = new Semaphore(0);

    /**
     * The reason for failure if it occurs
     */
    protected Throwable cause;

    /**
     * Trigger model updates on the Android main thread.
     *
     * @param values Each progress update entry represents a podcast to be added
     *               or removed locally, give <code>true</code> as the entry's key
     *               to add and <code>false</code> to remove.
     */
    @SafeVarargs
    @Override
    protected final void onProgressUpdate(Map.Entry<Boolean, Podcast>... values) {
        for (Map.Entry<Boolean, Podcast> entry : values) {
            // so we do just that:
            final boolean add = entry.getKey();
            final Podcast podcast = entry.getValue();

            if (add) {
                // We need to load the podcast before adding it
                // because otherwise we do not have its name
                new LoadPodcastTask(new OnLoadPodcastListener() {

                    @Override
                    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
                        // pass
                    }

                    @Override
                    public void onPodcastMoved(Podcast podcast, String newUrl) {
                        // pass, this will not be called if any sync controllers are active
                    }

                    @Override
                    public void onPodcastLoaded(Podcast podcast) {
                        podcastManager.addPodcast(podcast);
                        allPodcastLoadsFinishedSemaphore.release();
                    }

                    @Override
                    public void onPodcastLoadFailed(Podcast podcast, LoadPodcastTask.PodcastLoadError code) {
                        // Bad podcast, do not add
                        allPodcastLoadsFinishedSemaphore.release();
                    }
                }).executeOnExecutor(SyncController.SYNC_EXECUTOR, podcast);
            } else
                podcastManager.removePodcast(podcastManager.indexOf(podcast));
        }
    }
}
