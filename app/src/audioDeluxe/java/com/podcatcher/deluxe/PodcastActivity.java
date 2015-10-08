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

package com.podcatcher.deluxe;

import com.podcatcher.deluxe.listeners.OnChangePodcastListListener;
import com.podcatcher.deluxe.listeners.OnLoadPodcastListListener;
import com.podcatcher.deluxe.listeners.OnSyncListener;
import com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.view.fragments.AuthorizationFragment;
import com.podcatcher.deluxe.view.fragments.EpisodeListFragment;
import com.podcatcher.deluxe.view.fragments.PodcastListFragment;
import com.podcatcher.deluxe.view.fragments.PodcastListFragment.LogoViewMode;

import android.app.DialogFragment;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements OnBackStackChangedListener,
        OnLoadPodcastListListener, OnChangePodcastListListener, OnSyncListener {

    /**
     * The request code to identify import calls
     */
    private static final int IMPORT_FROM_SIMPLE_PODCATCHER_CODE = 18;
    /**
     * The import from Simple Podcatcher action
     */
    private static final String IMPORT_ACTION = "com.podcatcher.deluxe.action.IMPORT";

    /**
     * The current podcast list fragment
     */
    protected PodcastListFragment podcastListFragment;

    /**
     * Flag indicating whether the app should show the add podcast dialog if the
     * list of podcasts is empty.
     */
    private boolean isInitialAppStart = false;
    /**
     * Flag indicating the intent given onCreate contains data we want to use as
     * a podcast URL.
     */
    private boolean hasPodcastToAdd = false;

    /**
     * Flag indicating that the onResume() method has to make sure the UI
     * matches the current selection state.
     */
    private boolean needsUiUpdateOnResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strict mode when on debug
        if (BuildConfig.DEBUG)
            StrictMode.enableDefaults();

        // 1. Create the UI via XML layouts and fragments
        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);
        // Make sure all fragment member handles are properly set
        findFragments();
        // Add extra fragments needed in some view modes
        plugFragments();

        // 2. Register listeners (done after the fragments are available so we
        // do not end up getting call-backs without the possibility to act on
        // them).
        registerListeners();

        // 3. Init/restore the app as needed
        // If we are newly starting up and the podcast list is empty, show add
        // podcast dialog (this is used in onPodcastListLoaded(), since we only
        // know then, whether the list is actually empty). Also do not show it
        // if we are given an URL in the intent, because this will trigger the
        // dialog anyway.
        isInitialAppStart = (savedInstanceState == null);
        hasPodcastToAdd = (getIntent().getData() != null);
        needsUiUpdateOnResume = !isInitialAppStart;
        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null) {
            onPodcastListLoaded(podcastList, null);

            // We only reset our state if the podcast list is available, because
            // otherwise we will not be able to select anything.
            if (getIntent().hasExtra(MODE_KEY))
                onNewIntent(getIntent());
        }

        // Finally we might also be called freshly with a podcast feed to add
        if (getIntent().getData() != null)
            onNewIntent(getIntent());
    }

    @Override
    protected void findFragments() {
        super.findFragments();

        // The podcast list fragment to use
        if (podcastListFragment == null)
            podcastListFragment = (PodcastListFragment) findByTagId(R.string.podcast_list_fragment_tag);
    }

    /**
     * In certain view modes, we need to add some fragments because they are not
     * set in the layout XML files. Member variables will be set if needed.
     */
    private void plugFragments() {
        // On small screens, add the podcast list fragment
        if (view.isSmall() && podcastListFragment == null) {
            podcastListFragment = new PodcastListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, podcastListFragment,
                            getString(R.string.podcast_list_fragment_tag))
                    .commit();
        }
        // On small screens in landscape mode, add the episode list fragment
        if (view.isSmallLandscape() && episodeListFragment == null) {
            episodeListFragment = new EpisodeListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.right, episodeListFragment,
                            getString(R.string.episode_list_fragment_tag))
                    .commit();
        }
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();

        // Register as listener to the podcast data and sync managers
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);
        syncManager.addSyncListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This is an internal call to update the selection
        if (intent.hasExtra(MODE_KEY)) {
            final String podcastUrl = intent.getStringExtra(PODCAST_URL_KEY);
            final String episodeUrl = intent.getStringExtra(EPISODE_URL_KEY);

            selection.setMode((ContentMode) intent.getSerializableExtra(MODE_KEY));
            selection.setPodcast(podcastManager.findPodcastForUrl(podcastUrl));
            selection.setEpisode(podcastManager.findEpisodeForUrl(episodeUrl, podcastUrl));

            needsUiUpdateOnResume = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Prevent duplicate login dialog
        final DialogFragment authFragment = (DialogFragment)
                getFragmentManager().findFragmentByTag(AuthorizationFragment.TAG);

        if (view.isSmallPortrait() && authFragment != null)
            authFragment.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (needsUiUpdateOnResume) {
            needsUiUpdateOnResume = false;

            // Restore UI to match selection:
            // Re-select previously selected podcast(s)
            if (selection.isAll())
                onAllPodcastsSelected(true, false);
            else if (selection.isSingle() && selection.isPodcastSet())
                onPodcastSelected(selection.getPodcast(), true, false);
            else if (ContentMode.DOWNLOADS.equals(selection.getMode()))
                onDownloadsSelected();
            else if (ContentMode.PLAYLIST.equals(selection.getMode()))
                onPlaylistSelected();
            else
                onNoPodcastSelected(true);

            // Re-select previously selected episode
            if (selection.isEpisodeSet())
                onEpisodeSelected(selection.getEpisode(), true);
            else
                onNoEpisodeSelected(true);
        }

        // Trigger sync event
        if (!((Podcatcher) getApplication()).isOnMeteredConnection())
            syncManager.syncAll();

        // Make sure we are alerted on back stack changes. This needs to be
        // added after re-selection of the current content.
        getFragmentManager().addOnBackStackChangedListener(this);
        // Set podcast logo view mode
        updateLogoViewMode();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Disable listener (would interfere with resume)
        getFragmentManager().removeOnBackStackChangedListener(this);

        // Make sure we persist the podcast manager state
        podcastManager.saveState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Make sure our http cache is written to disk
        ((Podcatcher) getApplication()).flushHttpCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listeners
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);
        syncManager.removeSyncListener(this);
    }

    @Override
    public void onBackStackChanged() {
        // This only needed in small landscape mode and in case
        // we go back to the episode list
        if (view.isSmallLandscape()
                && getFragmentManager().getBackStackEntryCount() == 0) {
            onNoEpisodeSelected();
        }
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList, Uri location) {
        // Make podcast list show
        podcastListFragment.setPodcastList(podcastList);

        // Make action bar show number of podcasts
        updateActionBar();

        // If podcast list is empty we try to import from Simple Podcatcher
        if (podcastManager.size() == 0 && isInitialAppStart && !hasPodcastToAdd) {
            try {
                Intent importFromSimple = new Intent(IMPORT_ACTION);
                startActivityForResult(importFromSimple, IMPORT_FROM_SIMPLE_PODCATCHER_CODE);
            } catch (ActivityNotFoundException ex) {
                // Simple Podcatcher is not installed, we do not need to call
                // onActivityResult() since the system will do this
            }
        }
        // If enabled, we run the "select all on start-up" action
        else if (podcastManager.size() > 0 && isInitialAppStart
                && ((Podcatcher) getApplication()).isOnline()
                && preferences.getBoolean(SettingsActivity.KEY_SELECT_ALL_ON_START, false)) {
            onAllPodcastsSelected();
            selection.setEpisodeFilterEnabled(true);
        }

        this.isInitialAppStart = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Only run this if we were called back from onPodcastListLoaded(). This
        // means that we have no podcasts available and this is app start-up time.
        if (requestCode == IMPORT_FROM_SIMPLE_PODCATCHER_CODE) {
            boolean hasEmptyPodcastList = true;

            // Find if we got some podcasts
            if (data != null && !BuildConfig.FIXED_BUNDLE) {
                final List<String> names = data.getStringArrayListExtra(getString(R.string.podcast_names_key));
                final List<String> urls = data.getStringArrayListExtra(getString(R.string.podcast_urls_key));
                // Yes, we got some podcasts from the Simple Podcatcher
                if (names != null && names.size() > 0) {
                    // Make sure dialog does not pop up
                    hasEmptyPodcastList = false;
                    // Import all podcasts
                    for (String name : names)
                        podcastManager.addPodcast(new Podcast(name, urls.get(names.indexOf(name))));
                }
            }

            // On the very first start of the app, show the first run dialog
            if (hasEmptyPodcastList && preferences.getBoolean(SettingsActivity.KEY_FIRST_RUN, true))
                startActivity(new Intent(this, FirstRunActivity.class));
        }
    }

    @Override
    public void onPodcastListLoadFailed(Uri inputFile, Exception error) {
        // Nothing to do here
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // Update podcast list
        podcastListFragment.addPodcast(podcast);
        // Update UI
        updateActionBar();

        switch (view) {
            case SMALL_PORTRAIT:
                // Nothing is selected, just show the new podcast list
                selection.reset();
                break;
            case SMALL_LANDSCAPE:
                // Select the new podcast...
                selection.resetEpisode();
                selection.setPodcast(podcast);
                // .. but only run selection onResume()
                needsUiUpdateOnResume = true;
                break;
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // Immediately select new podcast
                onPodcastSelected(podcast);
                break;
        }
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // Update podcast list
        podcastListFragment.removePodcast(podcast);
        // Update UI
        updateActionBar();

        // Reset selection if deleted
        if (podcast.equals(selection.getPodcast()))
            onNoPodcastSelected();
        else if (selection.isPodcastSet())
            onPodcastSelected(selection.getPodcast(), true, false);
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        onPodcastSelected(podcast, false, false);
    }

    protected void onPodcastSelected(Podcast podcast, boolean forceUiRefresh, boolean forceReload) {
        if (forceUiRefresh || !podcast.equals(selection.getPodcast())) {
            super.onPodcastSelected(podcast, forceReload);

            if (view.isSmallPortrait())
                showEpisodeListActivity();
            else
                // Select in podcast list
                podcastListFragment.select(podcastManager.indexOf(podcast));

            // Update UI
            updateLogoViewMode();
        }
    }

    @Override
    public void onAllPodcastsSelected() {
        onAllPodcastsSelected(false, false);
    }

    private void onAllPodcastsSelected(boolean forceUiRefresh, boolean forceReload) {
        if (forceUiRefresh || !selection.isAll()) {
            super.onAllPodcastsSelected(forceReload);

            // Prepare podcast list fragment
            podcastListFragment.selectAll();

            if (view.isSmallPortrait())
                showEpisodeListActivity();

            // Update UI
            updateLogoViewMode();
        }
    }

    @Override
    public void onDownloadsSelected() {
        super.onDownloadsSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectNone();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onPlaylistSelected() {
        super.onPlaylistSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectNone();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onNoPodcastSelected() {
        onNoPodcastSelected(false);
    }

    private void onNoPodcastSelected(boolean forceReload) {
        if (forceReload || selection.getPodcast() != null) {
            super.onNoPodcastSelected();

            // Reset podcast list fragment
            podcastListFragment.selectNone();
            // Update UI
            updateLogoViewMode();
        }
    }

    @Override
    public void onEpisodeListSwipeToRefresh() {
        if (!view.isSmallPortrait()) {
            if (selection.isSingle() && selection.getPodcast() != null)
                onPodcastSelected(selection.getPodcast(), true, true);
            else if (selection.isAll())
                onAllPodcastsSelected(true, true);
            else if (ContentMode.DOWNLOADS.equals(selection.getMode()))
                onDownloadsSelected();
            else if (ContentMode.PLAYLIST.equals(selection.getMode()))
                onPlaylistSelected();
            else
                episodeListFragment.setShowOverlayProgress(false);
        }
    }

    @Override
    public void onPodcastListSwipeToRefresh() {
        if (!syncManager.isSyncRunning())
            syncManager.syncAll();

        showToast(getString(R.string.pref_sync_title), Toast.LENGTH_SHORT);
    }

    @Override
    public void onSyncConfigChanged() {
        // Enable swipe refresh in podcast list if sync is active
        podcastListFragment.setEnableSwipeRefresh(syncManager.getActiveControllerCount() > 0);
    }

    @Override
    public void onSyncStarted() {
    }

    @Override
    public void onSyncCompleted() {
        podcastListFragment.alertRefreshComplete();
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // Only react on progress here, if the activity is visible
        if (!view.isSmallPortrait()) {
            super.onPodcastLoadProgress(podcast, progress);

            // We are in select all mode, show progress in podcast list
            if (selection.isAll())
                podcastListFragment.showProgress(podcastManager.indexOf(podcast), progress);
        }
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        podcastListFragment.refresh();
        updateLogoViewMode();

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast, PodcastLoadError code) {
        podcastListFragment.refresh();

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoadFailed(failedPodcast, code);
    }

    @Override
    public void onDownloadProgress(Episode episode, int percent) {
        // In small portrait mode, there is a separate episode list activity
        // that will handle this
        if (!view.isSmallPortrait())
            super.onDownloadProgress(episode, percent);
    }

    @Override
    public void onDownloadFailed(Episode episode, EpisodeDownloadError error) {
        super.onDownloadFailed(episode, error);

        switch (error) {
            case DESTINATION_NOT_WRITABLE:
                showToast(getString(R.string.download_failed_cannot_write, episode.getName()),
                        Toast.LENGTH_LONG);
                break;
            case NO_SPACE:
                showToast(getString(R.string.download_failed_no_space, episode.getName()),
                        Toast.LENGTH_LONG);
                break;
            case DOWNLOAD_APP_DISABLED:
                showToast(getString(R.string.download_failed_download_app_disabled,
                        episode.getName()), Toast.LENGTH_LONG);
                break;
            default:
                showToast(getString(R.string.download_failed, episode.getName()));
        }
    }

    /**
     * Update the logo view mode according to current app state.
     */
    protected void updateLogoViewMode() {
        LogoViewMode logoViewMode = LogoViewMode.NONE;

        if (view.isLargeLandscape() && selection.isSingle())
            logoViewMode = LogoViewMode.LARGE;
        else if (view.isSmallPortrait())
            logoViewMode = LogoViewMode.SMALL;

        podcastListFragment.updateLogo(logoViewMode);
    }

    @Override
    protected void updateActionBar() {
        // Disable the home button (only used in overlaying activities)
        getActionBar().setHomeButtonEnabled(false);

        if (!view.isSmall() && selection.isAll())
            updateActionBarSubtitleOnMultipleLoad();
        else
            contentSpinner.setSubtitle(null);
    }

    @Override
    protected void updateEpisodeMetadataUi() {
        if (!view.isSmallPortrait())
            super.updateEpisodeMetadataUi();
    }

    @Override
    protected void updateDownloadUi() {
        if (!view.isSmallPortrait())
            super.updateDownloadUi();
    }

    @Override
    protected void updatePlaylistUi() {
        if (!view.isSmallPortrait())
            super.updatePlaylistUi();
    }

    @Override
    protected void updateStateUi() {
        if (!view.isSmallPortrait())
            super.updateStateUi();

        podcastListFragment.refresh();
    }

    @Override
    protected void updatePlayerUi() {
        super.updatePlayerUi();

        if (view.isSmallPortrait()) {
            playerFragment.setLoadMenuItemVisibility(false, false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }

    private void showEpisodeListActivity() {
        // We need to launch a new activity to display the episode list
        Intent intent = new Intent(this, ShowEpisodeListActivity.class);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }
}
