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
package com.podcatcher.deluxe.services;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser.MediaItem;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * The podcast browser service. Allows Android Auto and Android Wear as well as
 * potentially other services to browse the app's offered content.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PodcastBrowserService extends MediaBrowserService {

    /**
     * The browser root tag
     */
    private static final String BROWSER_ROOT = "__ROOT__";
    /**
     * The downloads child tag
     */
    private static final String DOWNLOADS_ROOT = "__DOWNLOADS__";
    /**
     * The playlist child tag
     */
    private static final String PLAYLIST_ROOT = "__PLAYLIST__";
    /**
     * Maximum number of episodes shown in episode lists
     * (other than downloads or playlist).
     */
    private static final int EPISODE_LIST_LIMIT = 10;

    /**
     * Handle to currently running task (prevents gc from taking it away)
     */
    private PodcastBrowserAsyncTask currentTask;

    /**
     * The service handle
     */
    private PlayEpisodeService service;
    /**
     * The service connection callback
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((PlayEpisodeService.PlayServiceBinder) binder).getService();
            setSessionToken(service.getMediaSessionToken());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Connect to play service
        Intent serviceIntent = new Intent(getApplicationContext(), PlayEpisodeService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(connection);
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(BROWSER_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId, @NonNull final Result<List<MediaItem>> result) {
        result.detach();

        switch (parentMediaId) {
            case BROWSER_ROOT:
                onLoadRoot(result);
                break;
            case DOWNLOADS_ROOT:
                onLoadDownloads(result);
                break;
            case PLAYLIST_ROOT:
                onLoadPlaylist(result);
                break;
            default:
                onLoadPodcast(result, parentMediaId);
                break;
        }
    }

    private void onLoadRoot(final Result<List<MediaItem>> result) {
        this.currentTask = new PodcastBrowserAsyncTask() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    episodeManager.blockUntilEpisodeMetadataIsLoaded();
                } catch (InterruptedException e) {
                    cancel(true);
                }

                // Add Downloads entry
                if (episodeManager.getDownloadsSize() > 0)
                    items.add(new MediaItem(new MediaDescription.Builder()
                            .setMediaId(DOWNLOADS_ROOT)
                            .setTitle(getString(R.string.downloads))
                            .setSubtitle(getResources().getQuantityString(R.plurals.episodes,
                                    episodeManager.getDownloadsSize(), episodeManager.getDownloadsSize()))
                            .build(), MediaItem.FLAG_BROWSABLE));

                // Add Playlist entry
                if (episodeManager.getPlaylistSize() > 0)
                    items.add(new MediaItem(new MediaDescription.Builder()
                            .setMediaId(PLAYLIST_ROOT)
                            .setTitle(getString(R.string.playlist))
                            .setSubtitle(getResources().getQuantityString(R.plurals.episodes,
                                    episodeManager.getPlaylistSize(), episodeManager.getPlaylistSize()))
                            .build(), MediaItem.FLAG_BROWSABLE));

                // Build list of podcasts
                final List<Podcast> podcasts = podcastManager.getPodcastList();
                for (Podcast podcast : podcasts) {
                    final int episodeCount = podcast.getEpisodeCount();
                    final MediaDescription.Builder mediaDescription = new MediaDescription.Builder()
                            .setMediaId(podcast.getUrl())
                            .setTitle(podcast.getName())
                            .setSubtitle(episodeCount > 0 ? getResources()
                                    .getQuantityString(R.plurals.episodes, episodeCount, episodeCount) : null);

                    if (podcast.hasLogoUrl())
                        mediaDescription.setIconUri(Uri.parse(podcast.getLogoUrl()));

                    items.add(new MediaItem(mediaDescription.build(), MediaItem.FLAG_BROWSABLE));
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                result.sendResult(items);
            }
        };

        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onLoadDownloads(final Result<List<MediaItem>> result) {
        this.currentTask = new PodcastBrowserAsyncTask() {

            @Override
            protected Void doInBackground(Void... params) {
                // Build list of downloaded episodes
                final List<Episode> downloads = episodeManager.getDownloads();
                for (Episode download : downloads)
                    items.add(episodeToMediaItem(download));

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                result.sendResult(items);
            }
        };

        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onLoadPlaylist(final Result<List<MediaItem>> result) {
        this.currentTask = new PodcastBrowserAsyncTask() {

            @Override
            protected Void doInBackground(Void... params) {
                // Build list of episodes in playlist
                final List<Episode> playlist = episodeManager.getPlaylist();
                for (Episode episode : playlist)
                    items.add(episodeToMediaItem(episode));

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                result.sendResult(items);
            }
        };

        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onLoadPodcast(final Result<List<MediaItem>> result, final String podcastUrl) {
        this.currentTask = new PodcastBrowserAsyncTask() {

            /**
             * Thread sync latch used to wait for podcast loading
             */
            private CountDownLatch latch = new CountDownLatch(1);
            /**
             * The feed URL we are currently waiting for. Needs to
             * be a member because we have to update it onPodcastMoved()
             */
            private String loadingPodcastUrl = podcastUrl;

            private OnLoadPodcastListener listener = new OnLoadPodcastListener() {

                @Override
                public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
                    // pass
                }

                @Override
                public void onPodcastMoved(Podcast podcast, String newUrl) {
                    if (podcast.equalByUrl(loadingPodcastUrl))
                        loadingPodcastUrl = newUrl;
                }

                @Override
                public void onPodcastLoaded(Podcast podcast) {
                    // Make sure we got the right podcast
                    if (podcast.equalByUrl(loadingPodcastUrl)) {
                        if (podcast.getEpisodeCount() > 0) {
                            // Limit episode list to a low item number
                            final List<Episode> episodes = podcast.getEpisodes().subList(0,
                                    podcast.getEpisodeCount() < EPISODE_LIST_LIMIT ?
                                            podcast.getEpisodeCount() : EPISODE_LIST_LIMIT);

                            for (Episode episode : episodes)
                                items.add(episodeToMediaItem(episode));
                        }

                        // Items ready, let task send results
                        latch.countDown();
                    }
                }

                @Override
                public void onPodcastLoadFailed(Podcast podcast, LoadPodcastTask.PodcastLoadError code) {
                    if (podcast.equalByUrl(loadingPodcastUrl))
                        latch.countDown();
                }
            };

            @Override
            protected void onPreExecute() {
                // Needs to run on UI thread
                podcastManager.addLoadPodcastListener(listener);
                podcastManager.load(podcastManager.findPodcastForUrl(loadingPodcastUrl));
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    cancel(true);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                podcastManager.removeLoadPodcastListener(listener);
                result.sendResult(items);
            }
        };

        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private MediaItem episodeToMediaItem(Episode episode) {
        final Podcast podcast = episode.getPodcast();
        final MediaDescription.Builder mediaDescription = new MediaDescription.Builder()
                .setMediaId(episode.getMediaUrl())
                .setTitle(episode.getName())
                .setSubtitle(podcast.getName());

        if (podcast.hasLogoUrl())
            mediaDescription.setIconUri(Uri.parse(podcast.getLogoUrl()));

        return new MediaItem(mediaDescription.build(), MediaItem.FLAG_PLAYABLE);
    }

    private abstract class PodcastBrowserAsyncTask extends AsyncTask<Void, Void, Void> {

        /**
         * The media item list to return
         */
        protected List<MediaItem> items = new ArrayList<>();

        /**
         * The task's podcast manager handle
         */
        protected PodcastManager podcastManager = PodcastManager.getInstance();
        /**
         * The task's episode manager handle
         */
        protected EpisodeManager episodeManager = EpisodeManager.getInstance();

        @Override
        protected void onCancelled() {
            onPostExecute(null);
        }
    }
}