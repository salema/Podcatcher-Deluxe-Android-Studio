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


import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;
import com.podcatcher.labs.sync.podcare.types.Item;

import android.content.Context;
import android.os.AsyncTask;

import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An episode metadata sync controller for the Podcare service. This
 * operates by keeping track of the local changes to episodes and publishing
 * everything once {@link #syncEpisodeMetadata()} is called.
 */
abstract class PodcareEpisodeMetadataSyncController extends PodcarePodcastListSyncController {

    /**
     * The list of changes to sync out to the service
     */
    private Map<String, Item> actions = Collections.synchronizedMap(new HashMap<String, Item>());

    /**
     * The preference key for the last sync time stamp
     */
    private static final String PODCARE_LAST_SYNC_ACTIONS = "PODCARE_LAST_SYNC_ACTIONS";
    /**
     * The last sync time stamp we are using
     */
    private long lastSyncTimeStamp;

    /**
     * The sync running flag
     */
    private boolean syncRunning = false;
    /**
     * The ignore actions flag
     */
    private boolean ignoreNewActions = false;

    /**
     * Our async task triggering the actual sync machinery
     */
    private class SyncEpisodeMetadataTask extends AsyncTask<Void, Map.Entry<Episode, Item>, Void> {

        /**
         * The reason for failure if it occurs
         */
        private Throwable cause;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // 0. Wait for the episode metadata to be available in the
                // manager because otherwise we cannot update it
                episodeManager.blockUntilEpisodeMetadataIsLoaded();

                // 1. Get the episode actions from server and apply them to the
                // local model if the episode is actually present. We do give the last
                // sync time stamp, so only changed that occurred afterwards are returned.
                if (SyncMode.SEND_RECEIVE.equals(mode)) {
                    final long changesSince = lastSyncTimeStamp;
                    // Set timestamp before making the actual call
                    // to make sure we do not miss any action/changes
                    lastSyncTimeStamp = new Date().getTime() / 1000;
                    final List<Item> changes = podcare.getUpdatedEpisodes(connectId, changesSince);

                    // Go walk through changes and act on them
                    if (changes != null)
                        for (Item item : changes) {
                            // Only act if we know the podcast
                            // (needs to be decoded since gpodder send encoded URLs)
                            final String podcastUrl = item.getFeed();
                            if (podcastManager.findPodcastForUrl(podcastUrl) == null)
                                continue;

                            // Get us an episode
                            final EpisodeMetadata meta = new EpisodeMetadata();
                            meta.podcastUrl = podcastUrl;
                            final Episode episode = meta.marshalEpisode(item.getFile());
                            // Act on the episode action if in receive mode
                            if (episode != null)
                                //noinspection unchecked
                                publishProgress(new AbstractMap.SimpleEntry<>(episode, item));
                        }
                }

                // 2. Upload local changes and clear them from the local action
                // list, the actions triggered above are not included since the
                // controller set the ignoreNewActions flag before applying those.
                if (actions.size() > 0) {
                    final Map<String, Item> copy = new HashMap<>(actions);
                    podcare.updateEpisodes(connectId, new ArrayList<>(copy.values()));

                    // 3. Remove all actions already taken care of (we can call this
                    // here because the list is synchronized). Unless new action
                    // arrived while the controller was busy, the local action list
                    // should be empty afterwards.
                    for (String id : copy.keySet())
                        actions.remove(id);
                }
            } catch (Exception ex) {
                this.cause = ex;
                cancel(true);
            }

            return null;
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(Map.Entry<Episode, Item>... values) {
            // Go change local model as needed
            final Episode episode = values[0].getKey();
            final Item item = values[0].getValue();

            // Make sure we do not pick up the same action again
            ignoreNewActions = true;

            // Update progress
            if (item.getProgress() != null)
                try {
                    final int remoteValue = ParserUtils.unformatTime(item.getProgress());
                    final int localValue = episodeManager.getResumeAt(episode);
                    // Only act if the remote value differs by more than one sec
                    if (Math.abs(remoteValue - localValue) > 1000)
                        episodeManager.setResumeAt(episode, remoteValue);
                } catch (ParseException pe) {
                    // skip
                }

            // Update state
            if (item.isFinished()) {
                if (episodeManager.getResumeAt(episode) != 0)
                    episodeManager.setResumeAt(episode, null);
                if (!episodeManager.getState(episode))
                    episodeManager.setState(episode, true);
            }

            // Re-enable action monitoring
            ignoreNewActions = false;
        }

        protected void onPostExecute(Void nothing) {
            preferences.edit().putLong(PODCARE_LAST_SYNC_ACTIONS, lastSyncTimeStamp).apply();
            syncRunning = false;

            if (listener != null)
                listener.onSyncCompleted(getImpl());
        }

        @Override
        protected void onCancelled(Void nothing) {
            syncRunning = false;

            if (listener != null)
                listener.onSyncFailed(getImpl(), cause);
        }
    }

    protected PodcareEpisodeMetadataSyncController(Context context) {
        super(context);

        // Recover the last synced time stamp
        this.lastSyncTimeStamp = preferences.getLong(PODCARE_LAST_SYNC_ACTIONS, 0);
    }

    @Override
    public boolean isRunning() {
        return syncRunning || super.isRunning();
    }

    @Override
    protected synchronized void syncEpisodeMetadata() {
        if (!syncRunning) {
            syncRunning = true;

            new SyncEpisodeMetadataTask().executeOnExecutor(SYNC_EXECUTOR, (Void) null);
        }
    }

    @Override
    public void onStateChanged(Episode episode, boolean newState) {
        if (!ignoreNewActions && episode != null)
            onRecordChange(episode, newState, episodeManager.getResumeAt(episode));
    }

    @Override
    public void onResumeAtChanged(Episode episode, Integer millis) {
        if (!ignoreNewActions && episode != null)
            onRecordChange(episode, episodeManager.getState(episode), millis == null ? 0 : millis);
    }

    private synchronized void onRecordChange(Episode episode, boolean newState, int millis) {
        Item item;

        // Update existing action if present
        if (actions.containsKey(episode.getMediaUrl()))
            item = actions.get(episode.getMediaUrl());
        else {
            item = episodeToItem(episode);
            actions.put(episode.getMediaUrl(), item);
        }

        item.setFinished(newState);
        item.setProgress(formatTime(millis / 1000));
    }

    private Item episodeToItem(Episode episode) {
        final Item result = new Item();
        result.setFeed(episode.getPodcast().getUrl());
        result.setFile(episode.getMediaUrl());
        result.setTitle(episode.getName());
        result.setDuration(formatTime(episode.getDuration()));
        result.setGuid(episode.getGuid() == null ? episode.getMediaUrl() : episode.getGuid());

        return result;
    }

    private static String formatTime(int time) {
        final int hours = time / 3600;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, (time / 60) - 60 * hours, time % 60);
    }
}
