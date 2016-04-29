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

package com.podcatcher.deluxe.model.sync.dropbox;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.sync.SyncPodcastListTask;
import com.podcatcher.deluxe.model.types.Podcast;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * A sync controller for the Dropbox service dealing with the podcast list.
 */
abstract class DropboxPodcastListSyncController extends DropboxBaseSyncController {

    /**
     * The podcast feed file name inside our app's Dropbox folder
     */
    private static final String SUBSCRIPTION_LIST_FILE_PATH = "/Subscriptions.txt";
    /**
     * The key for the sync ever complete before flag
     */
    private static final String FIRST_SYNC_EVER_KEY = "dropbox_first_ever";

    /**
     * Header put at line 1 of the Dropbox subscription list file
     */
    private String feedFileHeader;
    /**
     * The sync running flag
     */
    private boolean syncRunning = false;

    protected DropboxPodcastListSyncController(Context context) {
        super(context);
        feedFileHeader = context.getString(R.string.sync_dropbox_feed_file_header);
    }

    public boolean isRunning() {
        return syncRunning;
    }

    @Override
    public synchronized void syncPodcastList() {
        if (!syncRunning) {
            syncRunning = true;

            new DropboxSyncPodcastListTask().executeOnExecutor(SYNC_EXECUTOR, (Void) null);
        }
    }

    private class DropboxSyncPodcastListTask extends SyncPodcastListTask {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final boolean firstSyncEver = preferences.getBoolean(FIRST_SYNC_EVER_KEY, true);
                final Set<String> addedLocally = getLocallyAddedPodcastUrls();
                final Set<String> removedLocally = getLocallyRemovedPodcastUrls();

                // On first sync, use the full list of podcast because we do not
                // want the server to overwrite all the subscriptions we already have.
                if (firstSyncEver)
                    for (Podcast podcast : podcastManager.getPodcastList())
                        addedLocally.add(podcast.getUrl());

                // 1. Get subscriptions currently stored on Dropbox
                final Map<String, String> serverSubscriptions = getServerSubscriptions();
                boolean serverNeedsUpdate = false;

                // 2. Check podcasts added locally to see if we need to add something
                for (String url : addedLocally) {
                    final Podcast podcast = podcastManager.findPodcastForUrl(url);

                    if (podcast != null && !serverSubscriptions.containsKey(podcast.getUrl())) {
                        serverSubscriptions.put(podcast.getUrl(), podcast.getName());
                        serverNeedsUpdate = true;
                    }
                }

                // 3. Check podcasts removed locally to see if we need to delete something
                for (String url : removedLocally)
                    serverNeedsUpdate |= serverSubscriptions.remove(url) != null;

                // 4. Update local model if enabled
                if (SyncMode.SEND_RECEIVE.equals(mode)) {
                    // 4a. Remove local podcasts not on server list
                    for (Podcast podcast : podcastManager.getPodcastList())
                        if (!serverSubscriptions.containsKey(podcast.getUrl())) {
                            //noinspection unchecked
                            publishProgress(new AbstractMap.SimpleEntry<>(false, podcast));
                            removedLocally.add(podcast.getUrl());
                        }

                    // 4b. Add remote podcasts not in local list
                    int loadCount = 0;
                    for (Map.Entry<String, String> entry : serverSubscriptions.entrySet()) {
                        final Podcast podcast = new Podcast(entry.getValue(), entry.getKey());

                        if (!podcastManager.contains(podcast)) {
                            // Increase running tasks counter
                            loadCount++;
                            //noinspection unchecked
                            publishProgress(new AbstractMap.SimpleEntry<>(true, podcast));
                            addedLocally.add(podcast.getUrl());
                        }
                    }

                    // 4c. Wait for all the LoadPodcastTasks triggered by adding podcasts
                    // to finish, otherwise we would mess up the local status
                    try {
                        allPodcastLoadsFinishedSemaphore.acquire(loadCount);
                    } catch (InterruptedException ie) {
                        // pass
                    }
                }

                // 5a. Fix missing names
                for (Map.Entry<String, String> entry : serverSubscriptions.entrySet())
                    if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                        final Podcast podcast = podcastManager.findPodcastForUrl(entry.getKey());

                        if (podcast != null) {
                            entry.setValue(podcast.getName());
                            serverNeedsUpdate = true;
                        }
                    }

                // 5b. Update Dropbox content
                if (serverNeedsUpdate)
                    putServerSubscriptions(serverSubscriptions);

                // 6. Finally, clean up local status. In particular, this will leave changes
                // in place that happened while this task ran, to be picked up by next execution
                clearAddRemoveSets(addedLocally, removedLocally);
            } catch (Throwable th) {
                this.cause = th;
                cancel(true);

                Log.d(TAG, "Sync failed!", th);
            }

            return null;
        }

        private Map<String, String> getServerSubscriptions() throws DbxException {
            final Map<String, String> subscriptions = new HashMap<>();
            DbxDownloader downloader = null;
            try {
                // Get subscription file and process it
                downloader = client.files().download(SUBSCRIPTION_LIST_FILE_PATH);
                Scanner scanner = new Scanner(downloader.getInputStream(), "UTF-8").useDelimiter("\\A");
                String[] lines = (scanner.hasNext() ? scanner.next() : "").split("\\r\\n?|\\n");

                for (String line : lines) {
                    String[] parts = line.split("\\s", 2);
                    // Each line has to have at least an URL, title is optional
                    if (line.startsWith("http")) {
                        final Podcast podcast = new Podcast(parts.length > 1 ? parts[1] : null, parts[0]);
                        subscriptions.put(podcast.getUrl(), podcast.getName());
                    }
                }
            } catch (DownloadErrorException dee) {
                // If our feed file does not yet exist, return empty map
                // Otherwise:
                if (!dee.errorValue.isPath())
                    throw dee;
            } finally {
                if (downloader != null)
                    downloader.close();
            }

            return subscriptions;
        }

        private void putServerSubscriptions(Map<String, String> subscriptions) throws DbxException, IOException {
            // Make sure feeds are nicely sorted A->Z
            final Map<String, String> sortedMap = sortByValue(subscriptions);

            // Create file content, one podcast per line
            final StringBuilder fileContent = new StringBuilder(feedFileHeader);
            for (Map.Entry<String, String> podcast : sortedMap.entrySet()) {
                fileContent.append(podcast.getKey());
                if (podcast.getValue() != null)
                    fileContent.append(' ').append(podcast.getValue());
                fileContent.append('\n');
            }

            // Upload to Dropbox, no conflict resolution
            final InputStream in = new ByteArrayInputStream(fileContent.toString().getBytes());
            client.files().uploadBuilder(SUBSCRIPTION_LIST_FILE_PATH)
                    .withMode(WriteMode.OVERWRITE).uploadAndFinish(in);
        }

        private Map<String, String> sortByValue(Map<String, String> map) {
            List<Map.Entry<String, String>> list = new LinkedList<>(map.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
                public int compare(Map.Entry<String, String> podcast1, Map.Entry<String, String> podcast2) {
                    return new Podcast(podcast1.getValue(), podcast1.getKey())
                            .compareTo(new Podcast(podcast2.getValue(), podcast2.getKey()));
                }
            });

            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : list)
                result.put(entry.getKey(), entry.getValue());

            return result;
        }

        protected void onPostExecute(Void result) {
            // Make sure the first ever flag is set once we ran successfully
            preferences.edit().putBoolean(FIRST_SYNC_EVER_KEY, false).apply();
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
