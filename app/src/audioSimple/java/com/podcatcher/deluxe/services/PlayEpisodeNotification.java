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
import com.podcatcher.deluxe.model.types.Episode;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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
     * The actions our notification offers
     */
    private final NotificationCompat.Action stopAction;
    private final NotificationCompat.Action rewindAction;
    private final NotificationCompat.Action playAction;
    private final NotificationCompat.Action pauseAction;
    private final NotificationCompat.Action forwardAction;

    /**
     * Our builder
     */
    private NotificationCompat.Builder notificationBuilder;

    private PlayEpisodeNotification(Context context) {
        this.context = context;

        // Create all the static intents and actions we need for every build
        appIntent = new Intent(context, PodcastActivity.class)
                .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final Intent stopIntent = new Intent(context, PlayEpisodeService.class);
        stopIntent.setAction(PlayEpisodeService.ACTION_STOP);
        stopAction = new NotificationCompat.Action(R.drawable.ic_media_stop, context.getString(R.string.stop),
                PendingIntent.getService(context, 0, stopIntent, FLAG_UPDATE_CURRENT));

        final Intent rewindIntent = new Intent(context, PlayEpisodeService.class);
        rewindIntent.setAction(PlayEpisodeService.ACTION_REWIND);
        rewindAction = new NotificationCompat.Action(R.drawable.ic_media_rewind, context.getString(R.string.rewind),
                PendingIntent.getService(context, 0, rewindIntent, FLAG_UPDATE_CURRENT));

        final Intent toggleIntent = new Intent(context, PlayEpisodeService.class);
        toggleIntent.setAction(PlayEpisodeService.ACTION_TOGGLE);
        playAction = new NotificationCompat.Action(R.drawable.ic_media_play, context.getString(R.string.play),
                PendingIntent.getService(context, 0, toggleIntent, FLAG_UPDATE_CURRENT));
        pauseAction = new NotificationCompat.Action(R.drawable.ic_media_pause, context.getString(R.string.pause),
                PendingIntent.getService(context, 0, toggleIntent, FLAG_UPDATE_CURRENT));

        final Intent forwardIntent = new Intent(context, PlayEpisodeService.class);
        forwardIntent.setAction(PlayEpisodeService.ACTION_FORWARD);
        forwardAction = new NotificationCompat.Action(R.drawable.ic_media_forward, context.getString(R.string.forward),
                PendingIntent.getService(context, 0, forwardIntent, FLAG_UPDATE_CURRENT));
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
     * Build a new notification. To update the progress on the notification, use
     * {@link #updateProgress(int, int)} instead.
     *
     * @param episode  The episode playing.
     * @param paused   Playback state, <code>true</code> for paused.
     * @param canSeek  If the currently played media is seekable.
     * @param position The current playback progress.
     * @param duration The length of the current episode.
     * @param session  The media session representing current playback.
     * @return The notification to display.
     */
    @NonNull
    public Notification build(Episode episode, boolean paused, boolean canSeek,
                              int position, int duration, MediaSessionCompat session) {
        // 0. Prepare the main intent (leading back to the app)
        appIntent.putExtra(PODCAST_URL_KEY, episode.getPodcast().getUrl());
        appIntent.putExtra(EPISODE_URL_KEY, episode.getMediaUrl());
        final PendingIntent backToAppIntent = PendingIntent.getActivity(context, 0,
                appIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // 1. Create the notification builder and set values
        notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder
                .setContentIntent(backToAppIntent)
                .setTicker(episode.getName())
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(episode.getName())
                .setContentText(episode.getPodcast().getName())
                .setWhen(0)
                .setProgress(duration, position, false)
                .setOngoing(true);

        // 2. Load large image if available, see onBitmapLoaded() below
        if (episode.getPodcast().hasLogoUrl())
            Picasso.with(context).load(episode.getPodcast().getLogoUrl())
                    .resizeDimen(android.R.dimen.notification_large_icon_width,
                            android.R.dimen.notification_large_icon_height)
                    .into(this);

        // 3. Add actions to notification
        notificationBuilder.addAction(stopAction);
        if (canSeek)
            notificationBuilder.addAction(rewindAction);
        if (paused)
            notificationBuilder.addAction(playAction);
        else
            notificationBuilder.addAction(pauseAction);
        if (canSeek)
            notificationBuilder.addAction(forwardAction);

        // 4. Apply other notification features
        NotificationCompat.MediaStyle style =
                new NotificationCompat.MediaStyle().setMediaSession(session.getSessionToken());
        // Make sure not to show rew/ff icons for live streams
        if (canSeek)
            style.setShowActionsInCompactView(1, 2, 3); // rewind, toggle play, forward
        else
            style.setShowActionsInCompactView(0, 1); // stop, toggle play

        notificationBuilder.setStyle(style);
        notificationBuilder.setColor(ContextCompat.getColor(context, R.color.theme_dark));
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationBuilder.setCategory(NotificationCompat.CATEGORY_TRANSPORT);

        return notificationBuilder.build();
    }

    /**
     * Update the last notification build with a new progress and duration and
     * rebuild it leaving all the other data intact. Only call this after build().
     *
     * @param position The new progress position.
     * @param duration The length of the current episode.
     * @return The updated notification to display or <code>null</code> if called before build().
     * @see #build(Episode, boolean, boolean, int, int, MediaSessionCompat)
     */
    @Nullable
    public Notification updateProgress(int position, int duration) {
        return notificationBuilder == null ? null :
                notificationBuilder.setProgress(duration, position, false).build();
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
