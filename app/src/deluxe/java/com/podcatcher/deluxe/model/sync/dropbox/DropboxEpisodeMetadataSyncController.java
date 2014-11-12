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

package com.podcatcher.deluxe.model.sync.dropbox;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.AbstractMap;
import java.util.Map.Entry;

/**
 * A sync controller for the dropbox service dealing with the episode state.
 */
abstract class DropboxEpisodeMetadataSyncController extends DropboxPodcastListSyncController {

    /**
     * The episode metadata table name
     */
    private static final String EPISODE_TABLE = "episodes";
    /**
     * The podcast url record in the metadata table
     */
    private static final String PODCAST_URL = "podcast_url";
    /**
     * The episode media url record in the metadata table
     */
    private static final String EPISODE_MEDIA_URL = "media_url";
    /**
     * The episode state record in the metadata table
     */
    private static final String EPISODE_STATE = "is_old";
    /**
     * The episode resume at record in the metadata table
     */
    private static final String EPISODE_RESUME_AT = "resume_at";
    /**
     * The indicator that the resume at info was reset
     */
    private static final int RESUME_AT_RESET = -1;

    /**
     * The episode table handle
     */
    private DbxTable episodeTable;

    /**
     * The sync running flag
     */
    private boolean syncRunning = false;

    /**
     * Our async task that does the actual work for us
     */
    private class ApplyEpisodeMetadataTask extends AsyncTask<Void, Entry<Episode, DbxRecord>, Void> {

        /**
         * The reason for failure if it occurs
         */
        private Throwable cause;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Wait for the episode metadata to be available in the
                // manager because otherwise we cannot update it
                episodeManager.blockUntilEpisodeMetadataIsLoaded();

                // Walk through all the records and see what we need to do
                for (DbxRecord record : episodeTable.query()) {
                    // Only act if we know the podcast
                    if (!record.hasField(PODCAST_URL) ||
                            podcastManager.findPodcastForUrl(record.getString(PODCAST_URL)) == null)
                        continue;

                    // This is the hard and slow work we do not want to perform
                    // on the main thread
                    final Episode episode = podcastManager.findEpisodeForUrl(
                            record.getString(EPISODE_MEDIA_URL), record.getString(PODCAST_URL));

                    // If the episode exists in this context, update metadata on
                    // the main thread
                    if (episode != null)
                        //noinspection unchecked
                        publishProgress(new AbstractMap.SimpleEntry<>(episode, record));
                }
            } catch (DbxException | NullPointerException | InterruptedException e) {
                this.cause = e;
                cancel(true);
            }

            return null;
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(Entry<Episode, DbxRecord>... values) {
            final Episode episode = values[0].getKey();
            final DbxRecord record = values[0].getValue();

            // Update the state (old/new) information
            if (record.hasField(EPISODE_STATE)) {
                final boolean remoteState = record.getBoolean(EPISODE_STATE);
                final boolean localState = episodeManager.getState(episode);

                if (localState != remoteState)
                    episodeManager.setState(episode, remoteState);
            }

            // Update the resume at information
            if (record.hasField(EPISODE_RESUME_AT)) {
                final long remoteValue = record.getLong(EPISODE_RESUME_AT);
                long localValue = episodeManager.getResumeAt(episode);
                // If the local value is zero, it means the same as reset in the
                // Dropbox sync controller context, so rewrite
                localValue = (localValue == 0 ? RESUME_AT_RESET : localValue);

                if (localValue != remoteValue)
                    episodeManager.setResumeAt(episode,
                            remoteValue == RESUME_AT_RESET ? null : (int) remoteValue);
            }
        }

        protected void onPostExecute(Void nothing) {
            syncRunning = false;

            listener.onSyncCompleted(getImpl());
        }

        @Override
        protected void onCancelled(Void nothing) {
            syncRunning = false;

            listener.onSyncFailed(getImpl(), cause);
        }
    }

    protected DropboxEpisodeMetadataSyncController(Context context) {
        super(context);

        // Since the store might not be available, we might not have a table
        // handle and need to catch NPEs in all actions below.
        if (store != null)
            this.episodeTable = store.getTable(EPISODE_TABLE);
    }

    @Override
    public boolean isRunning() {
        return syncRunning || super.isRunning();
    }

    @Override
    protected void syncEpisodeMetadata() {
        // For now, we only pull in what has been changed so far
        syncStore();

        // In a later stage, one could actually walk through all episodes here
        // and publish their state to the Dropbox data store. That way, even
        // changes done when the sync controller was inactive would be included.
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        super.onPodcastRemoved(podcast);

        // TODO Remove all episode metadata for the removed podcast here?
    }

    @Override
    public void onStateChanged(Episode episode, boolean newState) {
        try {
            // Find and alter record for given episode as needed
            final DbxRecord episodeRecord = findAndPrepareRecord(episode);
            // Only write to the record if the data actually changed
            if (!episodeRecord.hasField(EPISODE_STATE)
                    || episodeRecord.getBoolean(EPISODE_STATE) != newState)
                episodeRecord.set(EPISODE_STATE, newState);
        } catch (DbxException | NullPointerException e) {
            Log.d(TAG, "State for episode " + episode + " cannot be synced to Dropbox", e);
        }
    }

    @Override
    public void onResumeAtChanged(Episode episode, Integer millis) {
        try {
            // Find and alter record for given episode as needed
            final DbxRecord episodeRecord = findAndPrepareRecord(episode);
            final long newValue = (millis == null ? RESUME_AT_RESET : millis);
            // Only write to the record if the data actually changed
            if (!episodeRecord.hasField(EPISODE_RESUME_AT)
                    || episodeRecord.getLong(EPISODE_RESUME_AT) != newValue)
                episodeRecord.set(EPISODE_RESUME_AT, newValue);
        } catch (DbxException | NullPointerException e) {
            Log.d(TAG, "Resume at for episode " + episode + " cannot be synced to Dropbox", e);
        }
    }

    @Override
    public void onDownloadSuccess(Episode episode) {
        // pass, download events are not synced via Dropbox
    }

    @Override
    public void onDownloadDeleted(Episode episode) {
        // pass, deletion events are not synced via Dropbox
    }

    @Override
    protected synchronized void onSyncStoreComplete() {
        super.onSyncStoreComplete();

        // The sync is done. If in receive mode, update local model
        if (SyncMode.SEND_RECEIVE.equals(mode) && !syncRunning) {
            syncRunning = true;

            new ApplyEpisodeMetadataTask()
                    .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void) null);
        }
    }

    private DbxRecord findAndPrepareRecord(Episode episode) throws DbxException {
        final DbxRecord episodeRecord = episodeTable.getOrInsert(toRecordId(episode));

        // If this is a fresh record, insert episode id
        if (!episodeRecord.hasField(EPISODE_MEDIA_URL)) {
            // Include the podcast URL for easier clean-ups
            episodeRecord.set(PODCAST_URL, episode.getPodcast().getUrl());
            episodeRecord.set(EPISODE_MEDIA_URL, episode.getMediaUrl());
        }

        return episodeRecord;
    }

    private String toRecordId(Episode episode) {
        return toValidDataStoreId(episode.getMediaUrl());
    }
}
