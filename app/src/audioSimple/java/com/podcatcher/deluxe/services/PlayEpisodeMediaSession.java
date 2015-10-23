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
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.Utils;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.concurrent.TimeUnit;

import static android.content.Intent.EXTRA_KEY_EVENT;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DATE;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_FAST_FORWARD;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_REWIND;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP;

/**
 * Our media session implementation.
 */
public class PlayEpisodeMediaSession extends MediaSessionCompat implements Target {

    /**
     * The log tag
     */
    private static final String LOG_TAG = "PlayEpisodeMediaSession";
    /**
     * Our service handle
     */
    private final PlayEpisodeService service;

    /**
     * The media metadata builder handle, needs to be member
     * to allow for async logo loading.
     */
    private MediaMetadataCompat.Builder metadataBuilder;
    /**
     * Logo URL member to prevent the wrong logo from being used
     * when the async loading returns late/never.
     */
    private String lastLogoLoading;
    /**
     * Last playback state set.
     */
    private int lastPlaybackState = PlaybackStateCompat.STATE_NONE;

    /**
     * Create new media session.
     * @param service The {@link PlayEpisodeService} handle.
     */
    PlayEpisodeMediaSession(@NonNull PlayEpisodeService service) {
        super(service.getApplicationContext(), LOG_TAG,
                new ComponentName(service.getApplicationContext(), MediaButtonReceiver.class), null);

        this.service = service;

        setFlags(FLAG_HANDLES_TRANSPORT_CONTROLS | FLAG_HANDLES_MEDIA_BUTTONS);
        setCallback(new PlayEpisodeSessionCallback());
    }

    /**
     * Update the session's metadata information to match result
     * given by {@link PlayEpisodeService#getCurrentEpisode()}.
     */
    public void updateMetadata() {
        this.metadataBuilder = new MediaMetadataCompat.Builder();
        final Episode episode = service.getCurrentEpisode();

        if (episode != null) {
            metadataBuilder.putString(METADATA_KEY_MEDIA_ID, episode.getMediaUrl())
                    .putString(METADATA_KEY_TITLE, episode.getName())
                    .putString(METADATA_KEY_ARTIST, episode.getPodcast().getName())
                    .putString(METADATA_KEY_DATE, Utils.getRelativePubDate(episode))
                    .putLong(METADATA_KEY_DURATION, TimeUnit.SECONDS.toMillis(episode.getDuration()));

            // Load and show logo, onBitmapLoaded() below
            if (episode.getPodcast().hasLogoUrl()) {
                this.lastLogoLoading = episode.getPodcast().getLogoUrl();
                Picasso.with(service.getApplicationContext())
                        .load(episode.getPodcast().getLogoUrl())
                        .resizeDimen(R.dimen.notification_logo_size, R.dimen.notification_logo_size)
                        .into(this);
            }
        }

        setMetadata(metadataBuilder.build());
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        // Add the logo Picasso loaded async, leaving the other metadata in place
        if (lastLogoLoading != null && service.getCurrentEpisode() != null &&
                lastLogoLoading.equals(service.getCurrentEpisode().getPodcast().getLogoUrl())) {
            metadataBuilder.putBitmap(METADATA_KEY_ART, bitmap);
            setMetadata(metadataBuilder.build());
        }
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        // pass
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        // pass
    }

    /**
     * Update the play state and available actions
     * using the last state previously given.
     */
    public void updatePlayState() {
        updatePlayState(lastPlaybackState);
    }

    /**
     * Update the play state and available actions
     * using the last state previously given.
     *
     * @param state New state as of {@link android.media.session.PlaybackState}.
     */
    public void updatePlayState(int state) {
        this.lastPlaybackState = state;

        final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setState(state, service.getCurrentPosition(), 1.0f)
                .setActions(getAvailableActions())
                .setErrorMessage(service.getString(R.string.player_error));

        if (service.getCurrentEpisode() != null && service.getCurrentEpisode().getMediaUrl() != null)
            stateBuilder.setActiveQueueItemId(service.getCurrentEpisode().getMediaUrl().hashCode());

        if (service.getBufferedPosition() > 0)
            stateBuilder.setBufferedPosition(service.getBufferedPosition());

        setPlaybackState(stateBuilder.build());
    }

    private long getAvailableActions() {
        long actions = ACTION_PLAY_PAUSE | ACTION_STOP |
                ACTION_SKIP_TO_PREVIOUS | ACTION_PLAY_FROM_MEDIA_ID;

        if (service.isPlaying())
            actions |= ACTION_PAUSE;
        else if (service.isPrepared())
            actions |= ACTION_PLAY;

        if (service.canSeek())
            actions |= ACTION_REWIND | ACTION_FAST_FORWARD | ACTION_SEEK_TO;

        return actions;
    }

    private class PlayEpisodeSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            boolean consumed = super.onMediaButtonEvent(mediaButtonEvent);

            if (mediaButtonEvent != null) {
                final KeyEvent event = mediaButtonEvent.getParcelableExtra(EXTRA_KEY_EVENT);

                // Make sure this is an event we want to react on
                if (event != null && KeyEvent.ACTION_DOWN == event.getAction()
                        && event.getRepeatCount() == 0)
                    consumed = MediaButtonReceiver.handleMediaKeyCode(service, event.getKeyCode());
            }

            return consumed;
        }

        @Override
        public void onPlay() {
            service.startService(new Intent(PlayEpisodeService.ACTION_PLAY,
                    null, service, PlayEpisodeService.class));
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            // TODO
        }

        @Override
        public void onPause() {
            service.startService(new Intent(PlayEpisodeService.ACTION_PAUSE,
                    null, service, PlayEpisodeService.class));
        }

        @Override
        public void onSkipToNext() {
            service.startService(new Intent(PlayEpisodeService.ACTION_SKIP,
                    null, service, PlayEpisodeService.class));
        }

        @Override
        public void onSkipToPrevious() {
            service.startService(new Intent(PlayEpisodeService.ACTION_PREVIOUS,
                    null, service, PlayEpisodeService.class));
        }

        @Override
        public void onFastForward() {
            service.startService(new Intent(PlayEpisodeService.ACTION_FORWARD,
                    null, service, PlayEpisodeService.class));
        }

        @Override
        public void onRewind() {
            service.startService(new Intent(PlayEpisodeService.ACTION_REWIND,
                    null, service, PlayEpisodeService.class));
        }

        @Override
        public void onStop() {
            service.startService(new Intent(PlayEpisodeService.ACTION_STOP,
                    null, service, PlayEpisodeService.class));
        }

        @Override
        public void onSeekTo(long position) {
            // TODO Use command instead?
            service.seekTo((int) position);
        }
    }
}