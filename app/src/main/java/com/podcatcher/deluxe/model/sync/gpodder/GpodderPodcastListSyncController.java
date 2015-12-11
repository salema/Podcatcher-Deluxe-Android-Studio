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

package com.podcatcher.deluxe.model.sync.gpodder;

import com.podcatcher.deluxe.model.sync.SyncPodcastListTask;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.labs.sync.gpodder.types.Subscription;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A podcast list sync controller for the gpodder.net service. This operates by
 * keeping track of the local changes to the podcast list and merging everything
 * together once {@link #syncPodcastList()} is called.
 */
abstract class GpodderPodcastListSyncController extends GpodderBaseSyncController {

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

            new GpodderSyncPodcastListTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void) null);
        }
    }

    private class GpodderSyncPodcastListTask extends SyncPodcastListTask {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // 1. Find sync preferences. If this is the first sync we are
                // ever performing, we have to treat this as a special case.
                final String firstEverKey = FIRST_SYNC_EVER_KEY + deviceId;
                final boolean firstSyncEver = preferences.getBoolean(firstEverKey, true);

                // 2. Get the lists of locally added/deleted podcasts
                final Set<String> addedLocally = getLocallyAddedPodcastUrls();
                final Set<String> removedLocally = getLocallyRemovedPodcastUrls();
                // On first sync, use the full list of podcast because we do not
                // want the server to overwrite all the subscriptions we already have.
                if (firstSyncEver)
                    for (Podcast podcast : podcastManager.getPodcastList())
                        addedLocally.add(podcast.getUrl());

                if (firstSyncEver && SyncMode.SEND_ONLY.equals(mode))
                    // 3a. On first sync and send only, simply send out the current podcast list
                    client.putSubscriptions(deviceId, addedLocally);
                else if (SyncMode.SEND_ONLY.equals(mode))
                    // 3b. On send only, update server with changes, no-op if both are empty
                    client.putSubscriptionChanges(deviceId, addedLocally, removedLocally);
                else {
                    // 3c. Sync mode is set to send/receive: consolidate local and remote lists
                    Set<String> synced = new HashSet<>();

                    // 3c1. Pull the subscription list for this device currently
                    // available on the gpodder.net server.
                    for (String url : client.getSubscriptions(deviceId))
                        // Make sure to use decoded and normalized URLs
                        synced.add(new Podcast(null, url).getUrl());
                    // Pull down all subscriptions if the device has never been synced.
                    // This will add all subscriptions from other devices to the local
                    // list so it does not end up empty.
                    if (firstSyncEver && synced.isEmpty())
                        for (Subscription subscription : client.getSubscriptions())
                            synced.add(new Podcast(null, subscription.getUrl()).getUrl());

                    // 3c2. Add/remove subscriptions to/from the set as needed,
                    // this will make sure that local modifications of the list
                    // will be preserved and pushed to the server.
                    synced.addAll(addedLocally);
                    synced.removeAll(removedLocally);

                    // 3c3. Remove local podcasts not in synced list
                    for (Podcast podcast : podcastManager.getPodcastList())
                        if (!synced.contains(podcast.getUrl())) {
                            //noinspection unchecked
                            publishProgress(new AbstractMap.SimpleEntry<>(false, podcast));
                            removedLocally.add(podcast.getUrl());
                        }

                    // 3c4. Add remote podcasts not in local list
                    int loadCount = 0;
                    for (String url : synced) {
                        final Podcast podcast = new Podcast(null, url);

                        if (!podcastManager.contains(podcast)) {
                            // Increase running tasks counter
                            loadCount++;
                            //noinspection unchecked
                            publishProgress(new AbstractMap.SimpleEntry<>(true, podcast));
                            addedLocally.add(url);
                        }
                    }

                    // 3c5. Push synced list (on first sync) or changes (later) to the server
                    if (firstSyncEver)
                        client.putSubscriptions(deviceId, synced);
                    else
                        client.putSubscriptionChanges(deviceId, addedLocally, removedLocally);

                    // 3c6. Wait for all the LoadPodcastTasks triggered by adding podcasts
                    // to finish, otherwise we would mess up the local status
                    try {
                        allPodcastLoadsFinishedSemaphore.acquire(loadCount);
                    } catch (InterruptedException ie) {
                        // pass
                    }
                }

                // 4. Finally, clean up local status. In particular, this will leave changes
                // in place that happened while this task ran, to be picked up by next execution
                clearAddRemoveSets(addedLocally, removedLocally);
            } catch (Throwable th) {
                this.cause = th;
                cancel(true);

                Log.d(TAG, "Sync failed!", th);
            }

            return null;
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
