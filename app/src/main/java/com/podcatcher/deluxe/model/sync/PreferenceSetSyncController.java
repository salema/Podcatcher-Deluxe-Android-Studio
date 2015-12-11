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

package com.podcatcher.deluxe.model.sync;

import com.podcatcher.deluxe.model.types.Podcast;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Optional and intermediate helper class to be sub-classed by sync controllers
 * that want to store changes in the local state in the Android preferences.
 * <p>
 * In particular, this class implements {@link #onPodcastAdded(Podcast)} and
 * {@link #onPodcastRemoved(Podcast)} and stores the additions and removals. Call
 * {@link #getLocallyAddedPodcastUrls()} and {@link #getLocallyRemovedPodcastUrls()}
 * to retrieve this information.
 * </p>
 * <p>
 * All methods are thread-safe.
 * </p>
 */
public abstract class PreferenceSetSyncController extends SyncController {

    /**
     * Our preferences handle
     */
    protected final SharedPreferences preferences;

    /**
     * The key for the list of added podcasts in the shared preferences
     */
    private final String addedSetKey = getImpl().getLabel() + "_added_subscriptions";
    /**
     * The key for the list of deleted podcasts in the shared preferences
     */
    private final String removedSetKey = getImpl().getLabel() + "_removed_subscriptions";


    protected PreferenceSetSyncController(Context context) {
        super();

        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        updateAddRemoveSets(podcast.getUrl(), true);
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        updateAddRemoveSets(podcast.getUrl(), false);
    }

    /**
     * @return The URLs of all podcasts added since the list has been last cleared.
     * @see #clearAddRemoveSets(Set, Set)
     */
    protected synchronized Set<String> getLocallyAddedPodcastUrls() {
        return new HashSet<>(preferences.getStringSet(addedSetKey, new HashSet<String>()));
    }

    /**
     * @return The URLs of all podcasts deleted since the list has been last cleared.
     * @see #clearAddRemoveSets(Set, Set)
     */
    protected synchronized Set<String> getLocallyRemovedPodcastUrls() {
        return new HashSet<>(preferences.getStringSet(removedSetKey, new HashSet<String>()));
    }

    /**
     * Remove all URLs given from the preferences. You want to call this after they
     * where processed by the sync controller and should not come up again. Changes that occurred
     * while sync was running (after you called {@link #getLocallyAddedPodcastUrls()} or
     * {@link #getLocallyRemovedPodcastUrls()}) will remain intacted and stored.
     *
     * @param added   URLs, whose addition has been synced.
     * @param removed URLs, whose deletion has been synced.
     */
    protected synchronized void clearAddRemoveSets(Set<String> added, Set<String> removed) {
        // Clean up the add/remove history after sync. (We cannot directly use
        // the sets coming from #getStringSet, see javadoc there.)
        final Set<String> subscriptionsAdded = new HashSet<>(
                preferences.getStringSet(addedSetKey, new HashSet<String>()));
        final Set<String> subscriptionsRemoved = new HashSet<>(
                preferences.getStringSet(removedSetKey, new HashSet<String>()));

        subscriptionsAdded.removeAll(added);
        subscriptionsRemoved.removeAll(removed);

        preferences.edit()
                .putStringSet(addedSetKey, subscriptionsAdded)
                .putStringSet(removedSetKey, subscriptionsRemoved)
                .apply();
    }

    private synchronized void updateAddRemoveSets(String podcastUrl, boolean add) {
        // We keep track of the changes to the local list and store them in the
        // app's preferences so they survive a restart. (We cannot directly use
        // the sets coming from #getStringSet, see javadoc there.)
        final Set<String> subscriptionsAdded = new HashSet<>(
                preferences.getStringSet(addedSetKey, new HashSet<String>()));
        final Set<String> subscriptionsRemoved = new HashSet<>(
                preferences.getStringSet(removedSetKey, new HashSet<String>()));

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
                .putStringSet(addedSetKey, subscriptionsAdded)
                .putStringSet(removedSetKey, subscriptionsRemoved)
                .apply();
    }
}
