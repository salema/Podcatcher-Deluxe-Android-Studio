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

package com.podcatcher.deluxe.model.sync.gpodder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A podcast list sync controller for the gpodder.net service. This operates by
 * keeping track of the local changes to the podcast list and merging everything
 * together once {@link #syncPodcastList()} is called.
 */
abstract class GpodderPodcastListSyncController extends GpodderBaseSyncController {

    /**
     * The key for the list of added podcasts in the shared preferences
     */
    private static final String REMOVED_KEY = "gpodder_removed_subscriptions";
    /**
     * The key for the list of deleted podcasts in the shared preferences
     */
    private static final String ADDED_KEY = "gpodder_added_subscriptions";
    /**
     * The key for the sync ever complete before flag
     */
    private static final String FIRST_SYNC_EVER_KEY = "gpodder_first_ever";

    /**
     * The sync running flag
     */
    private boolean syncRunning = false;

    protected GpodderPodcastListSyncController(Context context) {
        super(context);
    }

    @Override
    public boolean isRunning() {
        return syncRunning;
    }

    @Override
    public synchronized void syncPodcastList() {
        if (!syncRunning) {
            syncRunning = true;

            new SyncPodcastListTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void) null);
        }
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        updateAddRemoveSets(podcast.getUrl(), true);
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        updateAddRemoveSets(podcast.getUrl(), false);
    }

    private synchronized void updateAddRemoveSets(String podcastUrl, boolean add) {
        // We keep track of the changes to the local list and store them in the
        // app's preferences so they survive a restart. (We cannot directly use
        // the sets coming from #getStringSet, see javadoc there.)
        final Set<String> subscriptionsAdded = new HashSet<>(
                preferences.getStringSet(ADDED_KEY, new HashSet<String>()));
        final Set<String> subscriptionsRemoved = new HashSet<>(
                preferences.getStringSet(REMOVED_KEY, new HashSet<String>()));

        // We an action is reversed (add and then remove and vice versa), we can
        // delete that part of the history
        if (add) {
            subscriptionsAdded.add(podcastUrl);
            subscriptionsRemoved.remove(podcastUrl);
        } else {
            subscriptionsRemoved.add(podcastUrl);
            subscriptionsAdded.remove(podcastUrl);
        }

        preferences.edit()
                .putStringSet(ADDED_KEY, subscriptionsAdded)
                .putStringSet(REMOVED_KEY, subscriptionsRemoved)
                .apply();
    }

    private synchronized void clearAddRemoveSets(Set<String> added, Set<String> removed) {
        // Clean up the add/remove history after sync. (We cannot directly use
        // the sets coming from #getStringSet, see javadoc there.)
        final Set<String> subscriptionsAdded = new HashSet<>(
                preferences.getStringSet(ADDED_KEY, new HashSet<String>()));
        final Set<String> subscriptionsRemoved = new HashSet<>(
                preferences.getStringSet(REMOVED_KEY, new HashSet<String>()));

        subscriptionsAdded.removeAll(added);
        subscriptionsRemoved.removeAll(removed);

        preferences.edit()
                .putStringSet(ADDED_KEY, subscriptionsAdded)
                .putStringSet(REMOVED_KEY, subscriptionsRemoved)
                .apply();
    }

    /**
     * Our async task triggering the actual sync machinery
     */
    private class SyncPodcastListTask extends AsyncTask<Void, Entry<Boolean, Podcast>, Void> {

        /**
         * This task triggers some LoadPodcastTasks and should not finish before
         * these are all finished, so we need to track them.
         */
        private int runningLoadPodcastTaskCount = 0;

        /**
         * The reason for failure if it occurs
         */
        private Throwable cause;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // 1. Find sync preferences. If this is the first sync we are
                // ever performing, we have to push the current local list
                // (unless it is empty).
                final String firstEverKey = FIRST_SYNC_EVER_KEY + deviceId;
                final boolean firstSyncEver = preferences.getBoolean(firstEverKey, true);
                final boolean sendOnly = firstSyncEver && podcastManager.size() > 0;

                // 2. Create and fill the final subscription set to be synced
                // and equal locally and remotely once this task is done.
                // Filling it depends on the sync mode and the task's
                // preferences.
                Set<String> synced = new HashSet<>();
                // 2a. Simply send out the local list
                if (sendOnly || SyncMode.SEND_ONLY.equals(mode)) {
                    for (Podcast podcast : podcastManager.getPodcastList())
                        synced.add(podcast.getUrl());

                    client.putSubscriptions(deviceId, new ArrayList<>(synced));
                }
                // 2b. Sync mode is set to send/receive: consolidate local and
                // remote lists
                else {
                    // 2b1. Pull the subscription list for this device currently
                    // available on the gpodder.net server.
                    synced = client.getSubscriptions(deviceId);

                    // 2b2. Add/remove subscriptions to/from the set as needed,
                    // this will make sure local modifications of the list will
                    // be preserved and pushed to the server.
                    final Set<String> subscriptionsAddedLocally = new HashSet<>(
                            preferences.getStringSet(ADDED_KEY, new HashSet<String>()));
                    final Set<String> subscriptionsRemovedLocally = new HashSet<>(
                            preferences.getStringSet(REMOVED_KEY, new HashSet<String>()));
                    synced.addAll(subscriptionsAddedLocally);
                    synced.removeAll(subscriptionsRemovedLocally);

                    // 2b3. Remove local podcasts not in synced list
                    for (Podcast podcast : podcastManager.getPodcastList())
                        if (!synced.contains(podcast.getUrl())) {
                            //noinspection unchecked
                            publishProgress(new AbstractMap.SimpleEntry<>(false, podcast));
                            subscriptionsRemovedLocally.add(podcast.getUrl());
                        }

                    // 2b4. Add remote podcasts not in local list
                    for (String url : synced) {
                        final Podcast podcast = new Podcast(null, url);

                        if (!podcastManager.contains(podcast)) {
                            // Increase running tasks counter
                            runningLoadPodcastTaskCount++;
                            //noinspection unchecked
                            publishProgress(new AbstractMap.SimpleEntry<>(true, podcast));
                            subscriptionsAddedLocally.add(url);
                        }
                    }

                    // 2b5. Push synced list to the server
                    client.putSubscriptions(deviceId, new ArrayList<>(synced));

                    // 2b6. Wait for all the LoadPodcastTasks triggered by
                    // adding podcasts to finish, otherwise we would mess up the
                    // local status
                    while (runningLoadPodcastTaskCount > 0)
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (Exception e) {
                            // pass
                        }

                    // 2b7. Finally, clean up local status
                    clearAddRemoveSets(subscriptionsAddedLocally, subscriptionsRemovedLocally);
                }
            } catch (Throwable th) {
                this.cause = th;
                cancel(true);

                Log.d(TAG, "Sync failed!", th);
            }

            return null;
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(Entry<Boolean, Podcast>... values) {
            // Each progress update entry represents a podcast to be added or
            // removed locally, so we do just that:
            final boolean add = values[0].getKey();
            final Podcast podcast = values[0].getValue();

            if (add) {
                // We need to load the podcast before adding it
                // because otherwise we do not have its name
                new LoadPodcastTask(new OnLoadPodcastListener() {

                    @Override
                    public void onPodcastLoaded(Podcast podcast) {
                        podcastManager.addPodcast(podcast);
                        runningLoadPodcastTaskCount--;
                    }

                    @Override
                    public void onPodcastLoadProgress(Podcast p, Progress pr) {
                        // pass
                    }

                    @Override
                    public void onPodcastLoadFailed(Podcast p, PodcastLoadError code) {
                        runningLoadPodcastTaskCount--;
                        // Bad podcast, do not add
                    }
                }).executeOnExecutor(THREAD_POOL_EXECUTOR, podcast);
            } else
                podcastManager.removePodcast(podcastManager.indexOf(podcast));
        }

        protected void onPostExecute(Void result) {
            // Make sure the first ever flag is set once we ran successfully
            preferences.edit().putBoolean(FIRST_SYNC_EVER_KEY + deviceId, false).apply();
            // Update the local state
            syncRunning = false;

            if (listener != null)
                listener.onSyncCompleted(getImpl());
        }

        @Override
        protected void onCancelled(Void result) {
            syncRunning = false;

            if (listener != null)
                listener.onSyncFailed(getImpl(), cause);
        }
    }
}
