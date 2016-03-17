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

package com.podcatcher.deluxe.model.sync.podcare;

import com.podcatcher.deluxe.model.sync.SyncPodcastListTask;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.labs.sync.podcare.types.Subscription;

import android.content.Context;
import android.util.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A podcast list sync controller for the Podcare service. This operates by
 * keeping track of the local changes to the podcast list and merging everything
 * together once {@link #syncPodcastList()} is called.
 */
abstract class PodcarePodcastListSyncController extends PodcareBaseSyncController {

    /**
     * The key for the sync ever complete before flag
     */
    private static final String FIRST_SYNC_EVER_KEY = "podcare_first_ever";

    /**
     * The sync running flag
     */
    private boolean syncRunning = false;

    protected PodcarePodcastListSyncController(Context context) {
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

            new PodcareSyncPodcastListTask().executeOnExecutor(SYNC_EXECUTOR, (Void) null);
        }
    }

    private class PodcareSyncPodcastListTask extends SyncPodcastListTask {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // 1. Find sync preferences. If this is the first sync we are
                // ever performing, we have to push the current local list
                // (unless it is empty).
                final String firstEverKey = FIRST_SYNC_EVER_KEY + connectId;
                final boolean firstSyncEver = preferences.getBoolean(firstEverKey, true);

                // 2a. Simply send out the local list
                if (firstSyncEver && podcastManager.size() > 0) {
                    final List<Subscription> localList = new ArrayList<>(podcastManager.size());

                    for (Podcast podcast : podcastManager.getPodcastList())
                        localList.add(podcastToSubscription(podcast, Subscription.State.SUBSCRIBED));

                    podcare.addSubscriptions(connectId, localList);
                } else {
                    // 2b. Perform regular sync. First, send local changes out
                    final Set<String> subscriptionsAddedLocally = getLocallyAddedPodcastUrls();
                    final Set<String> subscriptionsRemovedLocally = getLocallyRemovedPodcastUrls();

                    // 2b1. Push new local subscriptions to Podcare
                    if (!subscriptionsAddedLocally.isEmpty()) {
                        final List<Subscription> feedsToUpload = new ArrayList<>(subscriptionsAddedLocally.size());

                        for (String feed : subscriptionsAddedLocally) {
                            final Podcast podcast = podcastManager.findPodcastForUrl(feed);
                            if (podcast != null)
                                feedsToUpload.add(podcastToSubscription(podcast, Subscription.State.SUBSCRIBED));
                        }

                        if (!feedsToUpload.isEmpty())
                            podcare.addSubscriptions(connectId, feedsToUpload);
                    }

                    // 2b2. Push deletions to Podcare
                    for (String feed : subscriptionsRemovedLocally) {
                        final Subscription subscription = new Subscription();
                        subscription.setFeed(feed);
                        subscription.setState(Subscription.State.DELETED);

                        podcare.updateSubscription(connectId, subscription);
                    }

                    if (SyncMode.SEND_RECEIVE.equals(mode)) {
                        // 2c. Sync mode is set to send/receive: consolidate local and
                        // remote lists
                        final Set<Podcast> synced = new HashSet<>();
                        // 2c1. Pull the subscription list for this device currently
                        // available on the Podcare server.
                        for (Subscription feed : podcare.getSubscriptions(connectId))
                            if (!Subscription.State.DELETED.equals(feed.getState()))
                                // Make sure to use decoded and normalized URLs
                                synced.add(new Podcast(feed.getTitle(), feed.getFeed()));

                        // 2c2. Remove local podcasts not in synced list
                        for (Podcast podcast : podcastManager.getPodcastList())
                            if (!synced.contains(podcast)) {
                                //noinspection unchecked
                                publishProgress(new AbstractMap.SimpleEntry<>(false, podcast));
                                subscriptionsRemovedLocally.add(podcast.getUrl());
                            }

                        // 2c3. Add remote podcasts not in local list
                        int loadCount = 0;
                        for (Podcast podcast : synced)
                            if (!podcastManager.contains(podcast)) {
                                // Increase running tasks counter
                                loadCount++;
                                //noinspection unchecked
                                publishProgress(new AbstractMap.SimpleEntry<>(true, podcast));
                                subscriptionsAddedLocally.add(podcast.getUrl());
                            }

                        // 2c4. Wait for all the LoadPodcastTasks triggered by adding podcasts
                        // to finish, otherwise we would mess up the local status
                        try {
                            allPodcastLoadsFinishedSemaphore.acquire(loadCount);
                        } catch (InterruptedException ie) {
                            // pass
                        }
                    }

                    // 2c5. Finally, clean up local status. In particular,
                    // this will leave changes in place that happened while
                    // this task ran, to be picked up by next execution
                    clearAddRemoveSets(subscriptionsAddedLocally, subscriptionsRemovedLocally);
                }
            } catch (Throwable th) {
                this.cause = th;
                cancel(true);

                Log.d(TAG, "Sync failed!", th);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Make sure the first ever flag is set once we ran successfully
            preferences.edit().putBoolean(FIRST_SYNC_EVER_KEY + connectId, false).apply();
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

        private Subscription podcastToSubscription(Podcast podcast, Subscription.State state) {
            final Subscription result = new Subscription();
            result.setFeed(podcast.getUrl());
            result.setTitle(podcast.getName());
            result.setState(state);

            return result;
        }
    }
}
