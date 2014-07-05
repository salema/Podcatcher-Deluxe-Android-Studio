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

package com.podcatcher.deluxe;

import android.app.DialogFragment;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.podcatcher.deluxe.listeners.OnChangePodcastListListener;
import com.podcatcher.deluxe.listeners.OnLoadPodcastListListener;
import com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.view.fragments.AuthorizationFragment;
import com.podcatcher.deluxe.view.fragments.EpisodeListFragment;
import com.podcatcher.deluxe.view.fragments.PodcastListFragment;
import com.podcatcher.deluxe.view.fragments.PodcastListFragment.LogoViewMode;

import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements OnBackStackChangedListener,
        OnLoadPodcastListListener, OnChangePodcastListListener {

    /**
     * The request code to identify import calls
     */
    private static final int IMPORT_FROM_SIMPLE_PODCATCHER_CODE = 18;
    /**
     * The import from Simple Podcatcher action
     */
    private static final String IMPORT_ACTION = "com.podcatcher.deluxe.action.IMPORT";
    /**
     * The key to find imported podcast name list under
     */
    private static final String IMPORT_PODCAST_NAMES_KEY = "podcast_names_key";
    /**
     * The key to find imported podcast url list under
     */
    private static final String IMPORT_PODCAST_URLS_KEY = "podcast_urls_key";

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
        if (((Podcatcher) getApplication()).isInDebugMode())
            StrictMode.enableDefaults();

        // 1. Create the UI via XML layouts and fragments
        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);
        // Make sure all fragment member handles are properly set
        findFragments();
        // Add extra fragments needed in some view modes
        plugFragments();
        // Make sure the podcast list knows about our theme colors.
        podcastListFragment.setThemeColors(themeColor, lightThemeColor);
        // Make sure the layout matches the preference
        updateLayout();

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
            episodeListFragment.setThemeColors(themeColor, lightThemeColor);

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

        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);
    }

    ;

    @Override
    protected void onNewIntent(Intent intent) {
        // This is an external call to add a new podcast
        if (intent.getData() != null) {
            Intent addPodcast = new Intent(this, AddPodcastActivity.class);
            addPodcast.setData(intent.getData());

            // We need to cut back the selection here when is small portrait
            // mode to prevent other activities from covering the add podcast
            // dialog
            if (view.isSmallPortrait())
                selection.reset();

            startActivity(addPodcast);
            // Reset data to prevent this intent from fire again on the next
            // configuration change
            intent.setData(null);
        }
        // This is an internal call to update the selection
        else if (intent.hasExtra(MODE_KEY)) {
            selection.setFullscreenEnabled(false);

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
                onAllPodcastsSelected(true);
            else if (selection.isSingle() && selection.isPodcastSet())
                onPodcastSelected(selection.getPodcast(), true);
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
        if (((Podcatcher) getApplication()).isOnFastConnection())
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Only run this if we were called back from onPodcastListLoaded(). This
        // means that we have no podcasts available and this is app start-up
        // time.
        if (requestCode == IMPORT_FROM_SIMPLE_PODCATCHER_CODE) {
            boolean needsAddPodcastDialog = true;

            // Find if we got some podcasts
            if (data != null) {
                final List<String> names = data.getStringArrayListExtra(IMPORT_PODCAST_NAMES_KEY);
                final List<String> urls = data.getStringArrayListExtra(IMPORT_PODCAST_URLS_KEY);
                // Yes, we got some podcasts from the Simple Podcatcher
                if (names != null && names.size() > 0) {
                    // Make sure dialog does not pop up
                    needsAddPodcastDialog = false;
                    // Import all podcasts
                    for (String name : names)
                        podcastManager.addPodcast(new Podcast(name, urls.get(names.indexOf(name))));
                }
            }

            // If nothing is there, show add podcasts dialog
            if (needsAddPodcastDialog) {
                isInitialAppStart = false;

                // On the very first start of the app, show the first run dialog
                if (preferences.getBoolean(SettingsActivity.KEY_FIRST_RUN, true))
                    startActivity(new Intent(this, FirstRunActivity.class));
                    // Otherwise, just show the add podcast dialog
                else
                    startActivity(new Intent(this, AddPodcastActivity.class));
            }
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
            onPodcastSelected(selection.getPodcast(), true);
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        onPodcastSelected(podcast, false);
    }

    private void onPodcastSelected(Podcast podcast, boolean forceReload) {
        if (forceReload || !podcast.equals(selection.getPodcast())) {
            super.onPodcastSelected(podcast);

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
        onAllPodcastsSelected(false);
    }

    private void onAllPodcastsSelected(boolean forceReload) {
        if (forceReload || !selection.isAll()) {
            super.onAllPodcastsSelected();

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
        // This will display the number of episodes
        podcastListFragment.refresh();

        // Tell the podcast manager to load podcast logo
        podcastManager.loadLogo(podcast);

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast, PodcastLoadError code) {
        podcastListFragment.refresh();

        // Tell the podcast manager to load podcast logo even though the podcast
        // failed to load since the podcast logo might be available offline.
        podcastManager.loadLogo(failedPodcast);

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoadFailed(failedPodcast, code);
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        super.onPodcastLogoLoaded(podcast);

        updateLogoViewMode();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        if (key.equals(SettingsActivity.KEY_THEME_COLOR) && podcastListFragment != null) {
            // Make the UI reflect the change
            podcastListFragment.setThemeColors(themeColor, lightThemeColor);
        } else if (key.equals(SettingsActivity.KEY_WIDE_EPISODE_LIST)) {
            updateLayout();
        }
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
     * Update the layout to match user's preference
     */
    protected void updateLayout() {
        final boolean useWide = preferences
                .getBoolean(SettingsActivity.KEY_WIDE_EPISODE_LIST, false);

        switch (view) {
            case LARGE_PORTRAIT:
                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);

                break;
            case LARGE_LANDSCAPE:
                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);
                setMainColumnWidthWeight(findViewById(R.id.right_column), useWide ? 3.5f : 4f);

                break;
            default:
                // Nothing to do in small views
                break;
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

    private void setMainColumnWidthWeight(View view, float weight) {
        view.setLayoutParams(
                new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight));
    }
}
