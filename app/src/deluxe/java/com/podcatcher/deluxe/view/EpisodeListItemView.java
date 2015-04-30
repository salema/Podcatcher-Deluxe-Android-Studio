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

package com.podcatcher.deluxe.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
     * The playlist position view
     */
    private TextView playlistPositionView;
    /**
     * The download icon view
     */
    private ImageView downloadIconView;
    /**
     * The download icon view
     */
    private ImageView resumeIconView;
    /**
     * The state icon view
     */
    private ImageView stateIconView;

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
        playlistPositionView = (TextView) findViewById(R.id.playlist_position);
        downloadIconView = (ImageView) findViewById(R.id.download_icon);
        resumeIconView = (ImageView) findViewById(R.id.resume_icon);
        stateIconView = (ImageView) findViewById(R.id.state_icon);
    }

    /**
     * Make the view update all its child to represent input given.
     *
     * @param episode         Episode to represent.
     * @param showPodcastName Whether the podcast name should show.
     * @param isOld           Whether the episode is consider old (listened to)
     */
    public void show(final Episode episode, boolean showPodcastName, boolean isOld) {
        // 0. Get episode state
        final boolean downloading = episodeManager.isDownloading(episode);
        final boolean progressShouldFade = episode.hashCode() == lastItemId;

        // 1. Set and format episode title
        titleTextView.setText(createTitle(episode));
        titleTextView.setTypeface(null, isOld ? Typeface.NORMAL : Typeface.BOLD);
        titleTextView.setSingleLine(isOld);

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
        updateMetadata(episode, isOld);

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
            // Remove podcast name from the episode title because it takes too
            // much space and is redundant anyway
            final String podcastName = episode.getPodcast().getName();
            if (result.startsWith(podcastName))
                result = result.substring(podcastName.length(), result.length());

            // Also remove all non-alphanumeric stuff at the start
            result = result.replaceFirst("^(\\s|,|:|-|–)+", "").trim();
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
        if (!showPodcastName && episode.getFileSize() > 0)
            builder.append(ParserUtils.formatFileSize(episode.getFileSize()));

        String result = builder.toString();
        if (result.endsWith(SEPARATOR))
            result = result.substring(0, result.length() - SEPARATOR.length());

        return result.isEmpty() ? NO_INFO : result;
    }

    private void updateMetadata(Episode episode, boolean isOld) {
        // Okay, so this gets a bit messy, we have a lot of cases to cover.
        // 1. Find all the information we need to make the view look right
        final boolean downloading = episodeManager.isDownloading(episode);
        final boolean downloaded = episodeManager.isDownloaded(episode);
        final boolean downloadIconShows = downloaded || downloading;
        final boolean willResume = episodeManager.getResumeAt(episode) > 0;
        final int position = episodeManager.getPlaylistPosition(episode);

        // 2. Set the view content and visibility accordingly
        if (downloading)
            downloadIconView.setImageResource(R.drawable.ic_media_downloading);
        else if (downloaded)
            downloadIconView.setImageResource(episodeManager.isDownloadedToSdCard(episode) ?
                    R.drawable.ic_sd_card : R.drawable.ic_internal_storage);

        playlistPositionView.setText(String.valueOf(position + 1));

        downloadIconView.setVisibility(downloading || downloaded ? View.VISIBLE : View.GONE);
        resumeIconView.setVisibility(willResume ? View.VISIBLE : View.GONE);
        playlistPositionView.setVisibility(position >= 0 ? View.VISIBLE : View.GONE);
        stateIconView.setVisibility(isOld ? View.GONE : View.VISIBLE);

        // 3. Fix the layout params of our views in the lower right corner
        // depending on the metadata showing:
        adjustLayout(playlistPositionView, true, -1);
        adjustLayout(resumeIconView, position < 0, R.id.playlist_position);
        adjustLayout(downloadIconView, position < 0 && !willResume,
                willResume ? R.id.resume_icon : R.id.playlist_position);

        // 4. Switch right hand anchor for the main content to whatever metadata
        // is showing:
        LayoutParams params = (RelativeLayout.LayoutParams) findViewById(
                R.id.list_item_main_content).getLayoutParams();
        params.addRule(RelativeLayout.LEFT_OF, downloadIconShows ? R.id.download_icon :
                willResume ? R.id.resume_icon : isOld ? R.id.playlist_position : R.id.state_icon);
        findViewById(R.id.list_item_main_content).setLayoutParams(params);
    }

    private void adjustLayout(View view, boolean atParentRight, int isLeftOf) {
        LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();

        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, atParentRight ? RelativeLayout.TRUE : 0);
        params.addRule(RelativeLayout.LEFT_OF, atParentRight ? -1 : isLeftOf);

        view.setLayoutParams(params);
    }
}
