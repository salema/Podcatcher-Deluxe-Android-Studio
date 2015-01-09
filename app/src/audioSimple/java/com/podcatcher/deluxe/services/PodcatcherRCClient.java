/** Copyright 2012-2015 Kevin Hausmann
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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RemoteControlClient;
import android.os.Build;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.Utils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.concurrent.TimeUnit;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DATE;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_TITLE;
import static android.media.RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK;

/**
 * Our remote control client used to provide playback information to a remote
 * control that might be present and able to display some episode metadata.
 */
public class PodcatcherRCClient extends RemoteControlClient implements Target {

    /**
     * The supported transport modes for the remote control
     */
    private static final int SUPPORTED_TRANSPORTS = FLAG_KEY_MEDIA_PLAY_PAUSE
            | FLAG_KEY_MEDIA_PAUSE | FLAG_KEY_MEDIA_PLAY | FLAG_KEY_MEDIA_STOP
            | FLAG_KEY_MEDIA_PREVIOUS | FLAG_KEY_MEDIA_REWIND | FLAG_KEY_MEDIA_FAST_FORWARD;

    /**
     * Create the remote control client.
     *
     * @param mediaButtonIntent The pending intent to use for the media buttons.
     * @param service           The episode playback service controlled by this remote.
     * @param episode           The episode to get metadata from.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public PodcatcherRCClient(PendingIntent mediaButtonIntent, final PlayEpisodeService service, Episode episode) {
        super(mediaButtonIntent);

        setTransportControlFlags();

        // On Android 4.3 and later we also add playback progress and scrubbing
        // support for the remote control client
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setOnGetPlaybackPositionListener(new OnGetPlaybackPositionListener() {

                @Override
                public long onGetPlaybackPosition() {
                    if (!service.isPrepared())
                        return -1;
                    else
                        return service.getCurrentPosition();
                }
            });
            setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {

                @Override
                public void onPlaybackPositionUpdate(long newPosition) {
                    service.seekTo((int) newPosition);

                    setPlaybackState(service.isPlaying() ? PLAYSTATE_PLAYING : PLAYSTATE_PAUSED,
                            service.getCurrentPosition(), 1.0f);
                }
            });
        }

        // Update the episode metadata
        setMetadata(episode);
        // Load and show logo, onBitmapLoaded() below
        if (episode.getPodcast().hasLogoUrl())
            Picasso.with(service.getApplicationContext())
                    .load(episode.getPodcast().getLogoUrl())
                    .resizeDimen(R.dimen.logo_size, R.dimen.logo_size)
                    .into(this);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        // Add the logo Picasso loaded async, leaving the other metadata in place
        editMetadata(false).putBitmap(BITMAP_KEY_ARTWORK, bitmap).apply();
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        // pass
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        // pass
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void setTransportControlFlags() {
        super.setTransportControlFlags(SUPPORTED_TRANSPORTS
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
                FLAG_KEY_MEDIA_POSITION_UPDATE : 0));
    }

    private void setMetadata(Episode episode) {
        final MetadataEditor editor = editMetadata(true);

        editor.putString(METADATA_KEY_TITLE, episode.getName())
                .putString(METADATA_KEY_ARTIST, episode.getPodcast().getName())
                .putString(METADATA_KEY_DATE, Utils.getRelativePubDate(episode))
                .putLong(METADATA_KEY_DURATION, TimeUnit.SECONDS.toMillis(episode.getDuration()));

        editor.apply();
    }
}
