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
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;

import com.podcatcher.deluxe.BaseActivity.ContentMode;
import com.podcatcher.deluxe.EpisodeListActivity;
import com.podcatcher.deluxe.PodcastActivity;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.HashMap;
import java.util.Map;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.podcatcher.deluxe.EpisodeActivity.EPISODE_URL_KEY;
import static com.podcatcher.deluxe.EpisodeListActivity.PODCAST_URL_KEY;

/**
 * Helper class for the {@link PlayEpisodeService} to encapsulate the complexity
 * of notifications.
 */
public class PlayEpisodeNotification {

    /**
     * The single instance
     */
    private static PlayEpisodeNotification instance;
    /**
     * The context the notifications live in
     */
    private final Context context;

    /**
     * Large icon width for the device (used for down-scaling)
     */
    private final int largeIconWidth;
    /**
     * Large icon height for the device (used for down-scaling)
     */
    private final int largeIconHeight;

    /**
     * The intent that brings back the app
     */
    private final Intent appIntent;
    /**
     * The pending intents for the actions
     */
    private final PendingIntent stopPendingIntent;
    private final PendingIntent tooglePendingIntent;

    /**
     * Our builder
     */
    private Notification.Builder notificationBuilder;
    /**
     * The cache for the scaled bitmaps
     */
    private Map<String, Bitmap> bitmapCache = new HashMap<>();

    private PlayEpisodeNotification(Context context) {
        this.context = context;

        final Resources res = context.getResources();
        this.largeIconWidth =
                (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        this.largeIconHeight =
                (int) res.getDimension(android.R.dimen.notification_large_icon_height);

        // Create all the static intents we need for every build
        appIntent = new Intent(context, PodcastActivity.class)
                .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final Intent stopIntent = new Intent(context, PlayEpisodeService.class);
        stopIntent.setAction(PlayEpisodeService.ACTION_STOP);
        stopPendingIntent = PendingIntent.getService(context, 0, stopIntent,
                FLAG_UPDATE_CURRENT);

        final Intent toogleIntent = new Intent(context, PlayEpisodeService.class);
        toogleIntent.setAction(PlayEpisodeService.ACTION_TOGGLE);
        tooglePendingIntent = PendingIntent.getService(context, 0, toogleIntent,
                FLAG_UPDATE_CURRENT);
    }

    /**
     * Get the single instance representing the service notification.
     *
     * @param context The context notifications should life in.
     * @return The single instance.
     */
    public static PlayEpisodeNotification getInstance(Context context) {
        if (instance == null)
            instance = new PlayEpisodeNotification(context);

        return instance;
    }

    /**
     * Build a new notification using default values for all but the episode.
     *
     * @param episode The episode playing.
     * @return The notification to display.
     * @see #build(Episode, boolean, int, int)
     */
    public Notification build(Episode episode) {
        return build(episode, false, 0, 0);
    }

    /**
     * Build a new notification. To update the progress on the notification, use
     * {@link #updateProgress(int, int)} instead.
     *
     * @param episode  The episode playing.
     * @param paused   Playback state, <code>true</code> for paused.
     * @param position The current playback progress.
     * @param duration The length of the current episode.
     * @return The notification to display.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public Notification build(Episode episode, boolean paused, int position, int duration) {
        // Prepare the main intent (leading back to the app)
        appIntent.putExtra(PODCAST_URL_KEY, episode.getPodcast().getUrl());
        appIntent.putExtra(EPISODE_URL_KEY, episode.getMediaUrl());
        final PendingIntent backToAppIntent = PendingIntent.getActivity(context, 0,
                appIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the notification builder and set values
        notificationBuilder = new Notification.Builder(context)
                .setContentIntent(backToAppIntent)
                .setTicker(episode.getName())
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(episode.getName())
                .setContentText(episode.getPodcast().getName())
                .setWhen(0)
                .setProgress(duration, position, false)
                .setOngoing(true);
        // Add large image if available
        if (episode.getPodcast().isLogoCached())
            notificationBuilder.setLargeIcon(getScaledBitmap(episode.getPodcast()));

        // Adding actions to notification is only supported in Android >4.1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Add stop action
            notificationBuilder.addAction(R.drawable.ic_media_stop,
                    context.getString(R.string.stop), stopPendingIntent);
            // Add other actions according to playback state
            if (paused)
                notificationBuilder.addAction(R.drawable.ic_media_resume,
                        context.getString(R.string.resume), tooglePendingIntent);
            else
                notificationBuilder.addAction(R.drawable.ic_media_pause,
                        context.getString(R.string.pause), tooglePendingIntent);
        }

        // This will call build(), not available before Android 4.1
        return notificationBuilder.getNotification();
    }

    /**
     * Update the last notification build with a new progress and duration and
     * rebuild it leaving all the other data intact. Only call this after having
     * called one of the build() methods before.
     *
     * @param position The new progress position.
     * @param duration The length of the current episode.
     * @return The updated notification to display.
     */
    public Notification updateProgress(int position, int duration) {
        notificationBuilder.setProgress(duration, position, false);

        // This will call build(), not available before Android 4.1
        return notificationBuilder.getNotification();
    }

    private Bitmap getScaledBitmap(Podcast podcast) {
        final String cacheKey = podcast.getUrl();

        if (!bitmapCache.containsKey(cacheKey))
            bitmapCache.put(cacheKey, Bitmap.createScaledBitmap(podcast.getLogo(),
                    largeIconWidth, largeIconHeight, false));

        return bitmapCache.get(cacheKey);
    }
}
