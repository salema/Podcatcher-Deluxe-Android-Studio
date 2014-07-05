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

package com.podcatcher.deluxe.services;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.media.RemoteControlClient;
import android.os.Build;

import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.Utils;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DATE;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_TITLE;
import static android.media.RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK;

/**
 * Our remote control client used to provide playback information to a remote
 * control that might be present and able to display some episode metadata.
 */
public class PodcatcherRCClient extends RemoteControlClient {

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
    public PodcatcherRCClient(PendingIntent mediaButtonIntent, final PlayEpisodeService service,
                              Episode episode) {

        super(mediaButtonIntent);

        // This will set the transport control flags
        showNext(!EpisodeManager.getInstance().isPlaylistEmptyBesides(episode));

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

        setMetadata(episode);
    }

    /**
     * Set whether the rc should show the option to skip ahead to the next
     * episode.
     *
     * @param canSkip Give <code>true</code> for the "next" transport control to
     *                be displayed.
     */
    public void showNext(boolean canSkip) {
        setTransportControlFlags(SUPPORTED_TRANSPORTS | (canSkip ? FLAG_KEY_MEDIA_NEXT : 0)
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
                FLAG_KEY_MEDIA_POSITION_UPDATE : 0));
    }

    private void setMetadata(Episode episode) {
        if (episode != null) {
            final MetadataEditor editor = editMetadata(true);

            editor.putString(METADATA_KEY_TITLE, episode.getName())
                    .putString(METADATA_KEY_ARTIST, episode.getPodcast().getName())
                    .putString(METADATA_KEY_DATE, Utils.getRelativePubDate(episode))
                    .putLong(METADATA_KEY_DURATION, episode.getDuration() * 1000);

            final Bitmap logo = episode.getPodcast().getLogo();
            if (logo != null)
                editor.putBitmap(BITMAP_KEY_ARTWORK, logo);

            editor.apply();
        }
    }
}
