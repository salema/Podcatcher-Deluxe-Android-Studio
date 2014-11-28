/** Copyright 2012-2014 Kevin Hausmann
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

package com.podcatcher.deluxe.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.types.Episode;

/**
 * A list item view to represent an episode.
 */
public class EpisodeListItemView extends PodcatcherListItemView {

    /**
     * String to use if there is no episode title available
     */
    private static final String NO_TITLE = "???";
    /**
     * String to use if there is no episode information available at all
     */
    private static final String NO_INFO = "---";
    /**
     * Separator for date and podcast name
     */
    private static final String SEPARATOR = " • ";

    /**
     * The title text view
     */
    private TextView titleTextView;
    /**
     * The caption text view
     */
    private TextView captionTextView;
    /**
     * The progress bar view
     */
    private ProgressBar progressBarView;
    /**
     * The download icon view
     */
    private ImageView downloadIconView;

    /**
     * Create an episode item list view.
     *
     * @param context Context for the view to live in.
     * @param attrs   View attributes.
     */
    public EpisodeListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        titleTextView = (TextView) findViewById(R.id.list_item_title);
        captionTextView = (TextView) findViewById(R.id.list_item_caption);
        progressBarView = (ProgressBar) findViewById(R.id.episode_list_item_progress);
        downloadIconView = (ImageView) findViewById(R.id.download_icon);
    }

    /**
     * Make the view update all its child to represent input given.
     *
     * @param episode         Episode to represent.
     * @param showPodcastName Whether the podcast name should show.
     */
    public void show(final Episode episode, boolean showPodcastName) {
        // 0. Get episode state
        final boolean downloading = episodeManager.isDownloading(episode);
        final boolean progressShouldFade = episode.hashCode() == lastItemId;

        // 1. Set and format episode title
        titleTextView.setText(createTitle(episode));

        // 2. Set caption and make sure it shows
        captionTextView.setText(createCaption(episode, showPodcastName));
        // If this is the same episode, crossfade (otherwise just set it)
        if (!downloading && isShowingProgress && progressShouldFade)
            crossfade(captionTextView, progressBarView);
        else
            captionTextView.setVisibility(downloading ? GONE : VISIBLE);

        // 3. Hide/show progress bar
        // If this is the same episode, crossfade (otherwise just set it)
        if (downloading && !isShowingProgress && progressShouldFade)
            crossfade(progressBarView, captionTextView);
        else
            progressBarView.setVisibility(downloading ? VISIBLE : GONE);
        // We need to reset the progress here, because the view might be
        // recycled and it should not show another episode's progress
        if (downloading)
            updateProgress(episodeManager.getDownloadProgress(episode));

        // 4. Update the metadata to show for this episode
        updateMetadata(episode);

        // 5. Store state to make sure it is available next time show() is
        // called and we can decide whether to crossfade or not
        this.isShowingProgress = downloading;
        this.lastItemId = episode.hashCode();
    }

    /**
     * Update the episode progress indicator to the progress given. Does not
     * change the visibility of the progress view.
     *
     * @param percent Progress to show.
     */
    public void updateProgress(int percent) {
        // Show progress in progress bar
        if (percent >= 0 && percent <= 100) {
            progressBarView.setIndeterminate(false);
            progressBarView.setProgress(percent);
        } else
            progressBarView.setIndeterminate(true);
    }

    private String createTitle(Episode episode) {
        String result = episode.getName();

        if (result != null && !result.trim().isEmpty()) {
            final String podcastName = episode.getPodcast().getName();

            final String redundantPrefix1 = podcastName + ": ";
            final String redundantPrefix2 = podcastName + " - ";
            final String redundantPrefix3 = podcastName + ", ";
            final String redundantPrefix4 = podcastName + " ";

            // Remove podcast name from the episode title because it takes too
            // much space and is redundant anyway
            if (result.startsWith(redundantPrefix1))
                result = result.substring(redundantPrefix1.length(), result.length());
            else if (result.startsWith(redundantPrefix2))
                result = result.substring(redundantPrefix2.length(), result.length());
            else if (result.startsWith(redundantPrefix3))
                result = result.substring(redundantPrefix3.length(), result.length());
            else if (result.startsWith(redundantPrefix4))
                result = result.substring(redundantPrefix4.length(), result.length());
        } else
            result = NO_TITLE;

        return result;
    }

    private String createCaption(Episode episode, boolean showPodcastName) {
        final StringBuilder builder = new StringBuilder();

        if (episode.getPubDate() != null)
            builder.append(Utils.getRelativePubDate(episode)).append(SEPARATOR);
        if (showPodcastName)
            builder.append(episode.getPodcast().getName()).append(SEPARATOR);
        if (!showPodcastName && episode.getDuration() > 0)
            builder.append(ParserUtils.formatTime(episode.getDuration())).append(SEPARATOR);
        if (!showPodcastName && episode.getMediaSize() > 0)
            builder.append(ParserUtils.formatFileSize(episode.getMediaSize()));

        String result = builder.toString();
        if (result.endsWith(SEPARATOR))
            result = result.substring(0, result.length() - SEPARATOR.length());

        return result.isEmpty() ? NO_INFO : result;
    }

    private void updateMetadata(Episode episode) {
        // Okay, so this gets a bit messy, we have a lot of cases to cover.
        // 1. Find all the information we need to make the view look right
        final boolean downloading = episodeManager.isDownloading(episode);
        final boolean downloaded = episodeManager.isDownloaded(episode);

        // 2. Set the view content and visibility accordingly
        if (downloading)
            downloadIconView.setImageResource(R.drawable.ic_media_downloading);
        else if (downloaded)
            downloadIconView.setImageResource(R.drawable.ic_internal_storage);

        downloadIconView.setVisibility(downloading || downloaded ? View.VISIBLE : View.GONE);
    }
}
