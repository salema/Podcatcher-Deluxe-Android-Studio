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

package com.podcatcher.deluxe;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.view.fragments.EpisodeListFragment;

import java.util.List;

/**
 * Activity to show only the episode list and possibly the player. Used in small
 * portrait view mode only.
 */
public class ShowEpisodeListActivity extends EpisodeListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In large or landscape layouts we do not need this activity at
        // all, so finish it. Also we need to avoid the case where the Android
        // system recreates this activity after the app has been killed and the
        // activity would show up with an endless progress indication because
        // there is no content selected.
        if (!view.isSmallPortrait() || (!selection.isAll() && !selection.isPodcastSet() &&
                !ContentMode.DOWNLOADS.equals(selection.getMode()) &&
                !ContentMode.PLAYLIST.equals(selection.getMode())))
            finish();
        else {
            // 1. Set the content view
            setContentView(R.layout.main);
            // 2. Set, find, create the fragments
            findFragments();
            // During initial setup, plug in the episode list fragment.
            if (savedInstanceState == null && episodeListFragment == null) {
                episodeListFragment = new EpisodeListFragment();

                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, episodeListFragment,
                                getString(R.string.episode_list_fragment_tag))
                        .commit();
            }

            // 3. Register the listeners needed to function as a controller
            registerListeners();

            // 4. Act according to selection
            if (selection.isAll())
                onAllPodcastsSelected();
            else if (ContentMode.DOWNLOADS.equals(selection.getMode()))
                onDownloadsSelected();
            else if (ContentMode.PLAYLIST.equals(selection.getMode()))
                onPlaylistSelected();
            else if (selection.isSingle() && selection.isPodcastSet())
                onPodcastSelected(selection.getPodcast());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateFilterUi();
        updateDownloadUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Deselect podcast (prevents the activity from being immediately re-opened)
            selection.resetPodcast();

            NavUtils.navigateUpFromSameTask(this);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);

            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Deselect podcast (prevents the activity from being immediately re-opened)
        selection.resetPodcast();

        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        onPodcastSelected(podcast, false);
    }

    @Override
    protected void onPodcastSelected(Podcast podcast, boolean forceReload) {
        super.onPodcastSelected(podcast, forceReload);

        // Init the list view...
        episodeListFragment.resetAndSpin();
        // ... and start loading
        podcastManager.load(podcast, forceReload);
        // ... plus special episodes
        episodeManager.getDownloadsAsync(this, podcast);
        episodeManager.getPlaylistAsync(this, podcast);
    }

    @Override
    public void onAllPodcastsSelected() {
        onAllPodcastsSelected(false);
    }

    public void onAllPodcastsSelected(boolean forceReload) {
        super.onAllPodcastsSelected(forceReload);

        // Init the list view...
        if (podcastManager.size() > 0)
            episodeListFragment.resetAndSpin();
        else
            episodeListFragment.resetUi();
        episodeListFragment.setShowPodcastNames(true);

        // ... plus special episodes...
        episodeManager.getDownloadsAsync(this);
        episodeManager.getPlaylistAsync(this);
        // ... and go get the podcast data
        for (Podcast podcast : podcastManager.getPodcastList())
            podcastManager.load(podcast, forceReload);

        updateActionBar();
    }

    @Override
    public void onDownloadsSelected() {
        super.onDownloadsSelected();

        episodeListFragment.resetAndSpin();
        episodeListFragment.setShowPodcastNames(true);

        episodeManager.getDownloadsAsync(this);
    }

    @Override
    public void onPlaylistSelected() {
        super.onPlaylistSelected();

        episodeListFragment.resetAndSpin();
        episodeListFragment.setShowPodcastNames(true);
        episodeListFragment.setEnableSwipeReorder(true);

        episodeManager.getPlaylistAsync(this);
    }

    @Override
    public void onPodcastListSwipeToRefresh() {
        // Nothing to do here.
    }

    @Override
    public void onEpisodeListSwipeToRefresh() {
        if (selection.isSingle() && selection.getPodcast() != null)
            onPodcastSelected(selection.getPodcast(), true);
        else if (selection.isAll())
            onAllPodcastsSelected(true);
        else if (ContentMode.DOWNLOADS.equals(selection.getMode()))
            onDownloadsSelected();
        else if (ContentMode.PLAYLIST.equals(selection.getMode()))
            onPlaylistSelected();
        else
            episodeListFragment.setShowOverlayProgress(false);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        super.onPodcastLoaded(podcast);

        updateProgress();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast, PodcastLoadError code) {
        super.onPodcastLoadFailed(failedPodcast, code);

        updateProgress();
    }

    @Override
    public void onDownloadsLoaded(List<Episode> downloads) {
        super.onDownloadsLoaded(downloads);

        if (!downloads.isEmpty())
            updateProgress();
    }

    @Override
    public void onDownloadSuccess(Episode episode) {
        // Reload list of downloads if one completes
        if (ContentMode.DOWNLOADS.equals(selection.getMode()))
            episodeManager.getDownloadsAsync(this);
        else
            updateDownloadUi();
    }

    @Override
    public void onPlaylistLoaded(List<Episode> playlist) {
        super.onPlaylistLoaded(playlist);

        if (!playlist.isEmpty())
            updateProgress();
    }

    @Override
    protected void updateActionBar() {
        contentSpinner.setTitle(getString(R.string.app_name));

        switch (selection.getMode()) {
            case SINGLE_PODCAST:
                if (!selection.isPodcastSet())
                    contentSpinner.setSubtitle(null);
                else {
                    if (selection.getPodcast().getEpisodes().isEmpty())
                        contentSpinner.setSubtitle(null);
                    else {
                        final int episodeCount = selection.getPodcast().getEpisodeCount();
                        contentSpinner.setSubtitle(getResources()
                                .getQuantityString(R.plurals.episodes, episodeCount, episodeCount));
                    }
                }
                break;
            case ALL_PODCASTS:
                updateActionBarSubtitleOnMultipleLoad();
                break;
            case DOWNLOADS:
                contentSpinner.setSubtitle(getString(R.string.downloads));
                break;
            case PLAYLIST:
                contentSpinner.setSubtitle(getString(R.string.playlist));
                break;
        }

        // Enable navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Update the progress indication for the list view.
     */
    protected void updateProgress() {
        // This should show if there is still a podcast loading.
        episodeListFragment.setShowOverlayProgress(selection.isAll()
                && podcastManager.getLoadCount() > 0);
    }

    @Override
    protected void updatePlayerUi() {
        super.updatePlayerUi();

        // Make sure to show episode title in player
        playerFragment.setLoadMenuItemVisibility(false, false, false);
        playerFragment.setPlayerTitleVisibility(true);
    }
}
