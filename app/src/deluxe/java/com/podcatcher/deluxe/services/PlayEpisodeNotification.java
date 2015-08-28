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

import com.podcatcher.deluxe.BaseActivity.ContentMode;
import com.podcatcher.deluxe.EpisodeListActivity;
import com.podcatcher.deluxe.PodcastActivity;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.podcatcher.deluxe.EpisodeActivity.EPISODE_URL_KEY;
import static com.podcatcher.deluxe.EpisodeListActivity.PODCAST_URL_KEY;

/**
 * Helper class for the {@link PlayEpisodeService} to encapsulate the complexity
 * of notifications.
 */
public class PlayEpisodeNotification implements Target {

    /**
     * The single instance
     */
    private static PlayEpisodeNotification instance;
    /**
     * The context the notifications live in
     */
    private final Context context;

    /**
     * The intent that brings back the app
     */
    private final Intent appIntent;
    /**
     * The pending intents for the actions
     */
    private final PendingIntent stopPendingIntent;
    private final PendingIntent rewindPendingIntent;
    private final PendingIntent togglePendingIntent;
    private final PendingIntent nextPendingIntent;
    private final PendingIntent forwardPendingIntent;

    /**
     * Our builder
     */
    private Notification.Builder notificationBuilder;

    private PlayEpisodeNotification(Context context) {
        this.context = context;

        // Create all the static intents we need for every build
        appIntent = new Intent(context, PodcastActivity.class)
                .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final Intent stopIntent = new Intent(context, PlayEpisodeService.class);
        stopIntent.setAction(PlayEpisodeService.ACTION_STOP);
        stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, FLAG_UPDATE_CURRENT);

        final Intent rewindIntent = new Intent(context, PlayEpisodeService.class);
        rewindIntent.setAction(PlayEpisodeService.ACTION_REWIND);
        rewindPendingIntent = PendingIntent.getService(context, 0, rewindIntent, FLAG_UPDATE_CURRENT);

        final Intent toggleIntent = new Intent(context, PlayEpisodeService.class);
        toggleIntent.setAction(PlayEpisodeService.ACTION_TOGGLE);
        togglePendingIntent = PendingIntent.getService(context, 0, toggleIntent, FLAG_UPDATE_CURRENT);

        final Intent forwardIntent = new Intent(context, PlayEpisodeService.class);
        forwardIntent.setAction(PlayEpisodeService.ACTION_FORWARD);
        forwardPendingIntent = PendingIntent.getService(context, 0, forwardIntent, FLAG_UPDATE_CURRENT);

        final Intent nextIntent = new Intent(context, PlayEpisodeService.class);
        nextIntent.setAction(PlayEpisodeService.ACTION_SKIP);
        nextPendingIntent = PendingIntent.getService(context, 0, nextIntent, FLAG_UPDATE_CURRENT);
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
        return build(episode, false, 0, 0, null);
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
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
        // Load large image if available, see onBitmapLoaded() below
        if (episode.getPodcast().hasLogoUrl())
            Picasso.with(context).load(episode.getPodcast().getLogoUrl())
                    .resizeDimen(android.R.dimen.notification_large_icon_width,
                            android.R.dimen.notification_large_icon_height)
                    .into(this);

        // Adding actions to notification is only supported in Android >= 4.1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // STOP
            notificationBuilder.addAction(R.drawable.ic_media_stop,
                    context.getString(R.string.stop), stopPendingIntent);

            // REWIND, Android >= 5.0 only
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                notificationBuilder.addAction(R.drawable.ic_media_rewind, null, rewindPendingIntent);

            // PLAY/PAUSE, according to playback state
            if (paused)
                notificationBuilder.addAction(R.drawable.ic_media_resume,
                        context.getString(R.string.resume), togglePendingIntent);
            else
                notificationBuilder.addAction(R.drawable.ic_media_pause,
                        context.getString(R.string.pause), togglePendingIntent);

            // FAST FORWARD, Android >= 5.0 only
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                notificationBuilder.addAction(R.drawable.ic_media_forward, null, forwardPendingIntent);

            // NEXT, if playlist contains another episode
            if (!EpisodeManager.getInstance().isPlaylistEmptyBesides(episode))
                notificationBuilder.addAction(R.drawable.ic_media_next,
                        context.getString(R.string.next), nextPendingIntent);
        }

        // This will call build(), not available before Android 4.1
        return notificationBuilder.getNotification();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Notification build(Episode episode, boolean paused, int position, int duration, MediaSession session) {
        build(episode, paused, position, duration);

        // Apply new notification features available in Lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationBuilder.setCategory(Notification.CATEGORY_TRANSPORT);
            notificationBuilder.setStyle(new Notification.MediaStyle()
                    .setShowActionsInCompactView(1, 2, 3)  // rewind, toggle play, forward
                    .setMediaSession(session == null ? null : session.getSessionToken()));
            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.theme_dark));
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

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        // Set bitmap on the notification builder, this will be picked up
        // when updateProgress is called the next time
        notificationBuilder.setLargeIcon(bitmap);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        // pass
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        // pass
    }
}
