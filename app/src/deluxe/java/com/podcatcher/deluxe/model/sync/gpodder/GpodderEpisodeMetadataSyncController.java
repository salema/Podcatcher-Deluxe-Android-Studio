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

package com.podcatcher.deluxe.model.sync.gpodder;

import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.labs.sync.gpodder.types.EpisodeAction;
import com.podcatcher.labs.sync.gpodder.types.EpisodeAction.Action;

import android.content.Context;
import android.os.AsyncTask;

import java.text.DateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import static java.net.URLDecoder.decode;

/**
 * An episode metadata sync controller for the gpodder.net service. This
 * operates by keeping track of the local changes to episodes and publishing
 * everything once {@link #syncEpisodeMetadata()} is called.
 */
abstract class GpodderEpisodeMetadataSyncController extends GpodderPodcastListSyncController {

    /**
     * The list of changes to sync out to the service
     */
    private List<EpisodeAction> actions = Collections.synchronizedList(new ArrayList<EpisodeAction>());

    /**
     * The preference key for the last sync time stamp
     */
    private static final String GPODDER_LAST_SYNC_ACTIONS = "GPODDER_LAST_SYNC_ACTIONS";
    /**
     * The last sync time stamp we are using
     */
    private long lastSyncTimeStamp;

    /**
     * The date format the gpodder.net service understands
     */
    private final DateFormat gpodderTimeStampFormat;

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
    private class SyncEpisodeMetadataTask extends
            AsyncTask<Void, Entry<Episode, EpisodeAction>, Void> {

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

                final List<EpisodeAction> changes = new ArrayList<>();
                if (SyncMode.SEND_RECEIVE.equals(mode)) {
                    // 1. Get the episode actions from server and apply them to the local model
                    // if the episode is actually present. We give the last sync time stamp + 1,
                    // so only changed that occurred afterwards are returned.
                    client.getEpisodeActions(changes, lastSyncTimeStamp + 1);

                    // Go walk through actions and act on them
                    for (EpisodeAction action : changes) {
                        // Only act if we know the podcast,
                        // (needs decode since gpodder sends encoded URLs)
                        final String podcastUrl = decode(action.getPodcast(), "UTF8");
                        if (podcastManager.findPodcastForUrl(podcastUrl) == null)
                            continue;

                        // Get us an episode and perform change on main thread
                        final EpisodeMetadata meta = new EpisodeMetadata();
                        meta.podcastUrl = new Podcast(null, podcastUrl).getUrl();
                        final Episode episode = meta.marshalEpisode(decode(action.getEpisode(), "UTF8"));
                        //noinspection unchecked
                        publishProgress(new AbstractMap.SimpleEntry<>(episode, action));
                    }
                }

                // 2. Upload local changes and clear them from the local action
                // list, the actions triggered above are not included since the
                // controller set the ignoreNewActions flag before applying those.
                final List<EpisodeAction> copy = new ArrayList<>(actions);
                final long newTimeStamp = client.putEpisodeActions(copy);
                lastSyncTimeStamp = newTimeStamp > lastSyncTimeStamp ? newTimeStamp : lastSyncTimeStamp;

                // 3. Remove all actions already taken care of (we can call this
                // here because the list is synchronized). Unless new action
                // arrived while the controller was busy, the local action list
                // should be empty afterwards.
                actions.removeAll(changes);
                actions.removeAll(copy);
            } catch (Exception ex) {
                this.cause = ex;
                cancel(true);
            }

            return null;
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(Entry<Episode, EpisodeAction>... values) {
            // Go change local model as needed
            final Episode episode = values[0].getKey();
            final EpisodeAction episodeAction = values[0].getValue();

            // Make sure we do not pick up the same action again
            ignoreNewActions = true;
            switch (episodeAction.getAction()) {
                case PLAY:
                    // If no position is given there is nothing to do
                    if (episodeAction.getPosition() != null) {
                        final int remoteValue = episodeAction.getPosition() * 1000;
                        final int localValue = episodeManager.getResumeAt(episode);
                        // Only act if the remote value differs by more than one sec
                        if (Math.abs(remoteValue - localValue) > 1000)
                            episodeManager.setResumeAt(episode, remoteValue);
                    }
                    break;
                case RESET:
                    // Only act if the value is not set yet
                    if (episodeManager.getResumeAt(episode) != 0)
                        episodeManager.setResumeAt(episode, null);
                    if (!episodeManager.getState(episode))
                        episodeManager.setState(episode, true);
                    break;
                default:
                    break;
            }
            // Re-enable action monitoring
            ignoreNewActions = false;
        }

        protected void onPostExecute(Void nothing) {
            // Make sure we take note of the time stamp
            preferences.edit().putLong(GPODDER_LAST_SYNC_ACTIONS, lastSyncTimeStamp).apply();
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

    protected GpodderEpisodeMetadataSyncController(Context context) {
        super(context);

        // Create the date time format for the time stamp sent to gpodder.net
        this.gpodderTimeStampFormat = client.getTimeStampFormatter();
        // Recover the last synced time stamp (unformatted long)
        this.lastSyncTimeStamp = preferences.getLong(GPODDER_LAST_SYNC_ACTIONS, 0);
    }

    @Override
    public boolean isRunning() {
        return syncRunning || super.isRunning();
    }

    @Override
    protected synchronized void syncEpisodeMetadata() {
        if (!syncRunning) {
            syncRunning = true;

            new SyncEpisodeMetadataTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void) null);
        }
    }

    @Override
    public void onStateChanged(Episode episode, boolean newState) {
        // Limit the number of actions added here since this will make the whole
        // app hang here if too many episodes are marked old at once
        if (!ignoreNewActions && actions.size() < 100 && newState)
            actions.add(prepareAction(episode, Action.RESET, 0));
    }

    @Override
    public void onResumeAtChanged(Episode episode, Integer millis) {
        if (!ignoreNewActions)
            // Send "new" event when the "resume at" was reset
            if (millis == null)
                actions.add(prepareAction(episode, Action.RESET, 0));
            else
                updatePlayPosition(episode, millis / 1000);
    }

    @Override
    public void onDownloadSuccess(Episode episode) {
        actions.add(prepareAction(episode, Action.DOWNLOAD, 0));
    }

    @Override
    public void onDownloadDeleted(Episode episode) {
        actions.add(prepareAction(episode, Action.DELETE, 0));
    }

    private void updatePlayPosition(Episode episode, int seconds) {
        // Find existing action and update it
        for (EpisodeAction action : actions)
            if (action.getEpisode().equals(episode.getMediaUrl()) && Action.PLAY.equals(action.getAction())) {
                action.setTimestamp(gpodderTimeStampFormat.format(new Date()));
                action.setPosition(seconds);

                return;
            }

        // None found, add new action
        actions.add(prepareAction(episode, Action.PLAY, seconds));
    }

    private EpisodeAction prepareAction(Episode episode, Action action, int position) {
        final EpisodeAction result = new EpisodeAction();
        result.setPodcast(episode.getPodcast().getUrl());
        result.setEpisode(episode.getMediaUrl());
        result.setAction(action);
        result.setDevice(deviceId);
        result.setTimestamp(gpodderTimeStampFormat.format(new Date()));
        result.setStarted(Action.PLAY.equals(action) ? 0 : null);
        result.setPosition(Action.PLAY.equals(action) ? position : null);
        result.setTotal(Action.PLAY.equals(action) && episode.getDuration() > 0 ?
                episode.getDuration() : null);

        return result;
    }
}
