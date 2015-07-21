package com.podcatcher.deluxe.services;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@TargetApi(21)
public class PodcastBrowserService extends MediaBrowserService {

    private static final String TAG = "PodcastBrowserService";
    private static final String BROWSER_ROOT = "__ROOT__";
    static final String CURRENT_PLAYLIST_MEDIA_ID = "__PLAYLIST__";
    private static final String PODCAST_PREFIX = "__PODCAST__";
    static final String SHOW_ALL_SUFFIX = "#__SHOW__ALL__";

    private PlayEpisodeService mService;

    @Override
    public void onCreate() {
        super.onCreate();

        PodcastManager podcastManager = PodcastManager.getInstance();
        for (Podcast podcast : podcastManager.getPodcastList()) {
            // Start loading all data asynchronously.
            podcastManager.load(podcast);
        }

        Intent serviceIntent = new Intent(getApplicationContext(), PlayEpisodeService.class);
        startService(serviceIntent);
        bindService(serviceIntent, mConnection, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(BROWSER_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId, @NonNull final Result<List<MediaItem>> result) {
        List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();
        PodcastManager manager = PodcastManager.getInstance((Podcatcher) getApplication());
        if (BROWSER_ROOT.equals(parentMediaId)) {
            if (!EpisodeManager.getInstance().isPlaylistEmpty()) {
                mediaItems.add(new MediaItem(new MediaDescription.Builder()
                        .setMediaId(CURRENT_PLAYLIST_MEDIA_ID)
                        .setTitle(getString(R.string.playlist))
                        .build(), MediaItem.FLAG_PLAYABLE));
            }
            // Build list of podcasts.
            List<Podcast> podcasts = manager.getPodcastList();
            for (Podcast podcast : podcasts) {
                if (podcast.getEpisodeCount() > 0) {
                    MediaDescription.Builder mediaDescription = new MediaDescription.Builder()
                            .setMediaId(PODCAST_PREFIX + podcast.getUrl())
                            .setTitle(podcast.getName());
                    if (podcast.getLogoUrl() != null && podcast.getLogoUrl().isEmpty()) {
                        mediaDescription.setIconUri(Uri.parse(podcast.getLogoUrl()));
                    }
                    mediaItems.add(new MediaItem(mediaDescription.build(), MediaItem.FLAG_BROWSABLE));
                }
            }
        } else {
            // Build list of episodes.
            String podcastUri = parentMediaId.substring(PODCAST_PREFIX.length());
            boolean showAll = false;
            if (!parentMediaId.endsWith(SHOW_ALL_SUFFIX)) {
                mediaItems.add(new MediaItem(new MediaDescription.Builder()
                        .setMediaId(parentMediaId + SHOW_ALL_SUFFIX)
                        .setTitle(getString(R.string.show_all_label))
                        .build(), MediaItem.FLAG_BROWSABLE));
            } else {
                showAll = true;
                podcastUri = podcastUri.substring(0, parentMediaId.indexOf(SHOW_ALL_SUFFIX));
            }
            Podcast podcast = manager.findPodcastForUrl(podcastUri);
            if (podcast != null) {
                List<Episode> episodes = showAll ? podcast.getEpisodes() : podcast.getEpisodes(true, true);
                for (Episode episode : episodes) {
                    mediaItems.add(episodeToMediaItem(episode, showAll));
                }
            }
        }

        result.sendResult(mediaItems);
    }

    private MediaItem episodeToMediaItem(Episode episode, boolean showAll) {
        Podcast podcast = episode.getPodcast();
        MediaDescription.Builder mediaDescription = new MediaDescription.Builder()
                .setMediaId(episode.getMediaUrl() + (showAll ? SHOW_ALL_SUFFIX : ""))
                .setTitle(episode.toString())
                .setExtras(convertEpisodeToBundle(episode));
        if (podcast.getLogoUrl() != null && !podcast.getLogoUrl().isEmpty()) {
            mediaDescription.setIconUri(Uri.parse(podcast.getLogoUrl()));
        }
        if (episode.getDuration() > 0) {
            mediaDescription.setSubtitle(ParserUtils.formatTime(episode.getDuration()));
        }
        return new MediaItem(mediaDescription.build(), MediaItem.FLAG_PLAYABLE);
    }

    private Bundle convertEpisodeToBundle(Episode episode) {
        Bundle bundle = new Bundle();
        bundle.putString(MediaStore.EXTRA_MEDIA_ALBUM, episode.getPodcast().getName());
        bundle.putString(MediaStore.EXTRA_MEDIA_TITLE, episode.getName());
        return bundle;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((PlayEpisodeService.PlayServiceBinder) service).getService();
            setSessionToken((MediaSession.Token) mService.getMediaSession().getSessionToken().getToken());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
}
