/**
 * Copyright 2012-2016 Kevin Hausmann
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

import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.view.ViewMode;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Podcatcher base activity. Defines some common functionality useful for all
 * activities.
 */
public abstract class BaseActivity extends Activity implements OnSharedPreferenceChangeListener {

    /**
     * The podcatcher website URL
     */
    public static final String PODCATCHER_WEBSITE = "http://www.podcatcher-deluxe.com";
    /**
     * The podcatcher help website URL
     */
    public static final String PODCATCHER_HELPSITE = "http://www.podcatcher-deluxe.com/help";
    /**
     * The podcatcher help website URL (add anchor)
     */
    protected static final String PODCATCHER_HELPSITE_ADD = "http://www.podcatcher-deluxe.com/help#add";
    /**
     * Twitter account page URL
     */
    public static final String PODCATCHER_TWITTER = "http://twitter.com/PodcatchDeluxe";

    /**
     * Key to find the podcast positions under
     */
    public static final String PODCAST_POSITION_LIST_KEY = "position_list_key";

    /**
     * The amount of min-dp for the large screen bucket
     */
    public static final int MIN_PIXEL_LARGE = 600;

    /**
     * The podcast manager handle
     */
    protected PodcastManager podcastManager;
    /**
     * The episode manager handle
     */
    protected EpisodeManager episodeManager;
    /**
     * The sync manager handle
     */
    protected SyncManager syncManager;
    /**
     * The shared app preferences
     */
    protected SharedPreferences preferences;

    /**
     * Key to activity creation counter in preferences
     */
    private static final String APP_USAGE_COUNT_KEY = "app_usage_count";
    /**
     * Number of minimum activity creations to reveal the review menu item.
     */
    private static final int APP_USAGE_REVIEW_TRIGGER = 50;
    /**
     * Key to preference flag indicating whether the user ever tapped the review menu item
     */
    private static final String REVIEW_MENU_ITEM_CLICKED_KEY = "review_menu_item_clicked";
    /**
     * Key to preference flag indicating whether the user ever tapped the follow menu item
     */
    private static final String FOLLOW_MENU_ITEM_CLICKED_KEY = "follow_menu_item_clicked";

    /**
     * The currently active view mode
     */
    protected ViewMode view;
    /**
     * The currently active selection
     */
    protected ContentSelection selection;

    /**
     * Our toast object
     */
    private Toast toast;

    /**
     * The options available for the content mode
     */
    public enum ContentMode {
        /**
         * Show single podcast
         */
        SINGLE_PODCAST,

        /**
         * Show all podcast
         */
        ALL_PODCASTS,

        /**
         * Show downloads
         */
        DOWNLOADS,

        /**
         * Show playlist
         */
        PLAYLIST
    }

    /**
     * Content selection singleton, makes the user selection of podcasts,
     * episodes, etc. available to all activities across activity recreations.
     */
    protected static class ContentSelection {
        /**
         * The single instance
         */
        private static ContentSelection instance;

        /**
         * Flag to indicate the content mode
         */
        private ContentMode mode = ContentMode.SINGLE_PODCAST;

        /**
         * The podcast we are showing episodes for
         */
        private Podcast currentPodcast;
        /**
         * The selected episode
         */
        private Episode currentEpisode;

        /**
         * The sorting reversed flag
         */
        private boolean sortingReversed = false;
        /**
         * The filter active flag
         */
        private boolean filterEnabled = false;
        /**
         * The fullscreen mode flag
         */
        private boolean fullscreen = false;

        private ContentSelection() {
            // Nothing to do here
        }

        /**
         * Get the single instance representing the current user selection in
         * the app.
         *
         * @return The single instance.
         */
        public static ContentSelection getInstance() {
            if (instance == null)
                instance = new ContentSelection();

            return instance;
        }

        /**
         * @return The currently selected mode. Default and init state is single
         * podcast.
         * @see ContentMode
         */
        public ContentMode getMode() {
            return mode;
        }

        /**
         * @param mode The mode to set.
         * @see ContentMode
         */
        public void setMode(ContentMode mode) {
            this.mode = mode;
        }

        /**
         * @return The currently selected podcast. Might be <code>null</code> to
         * indicate "no selection".
         */
        public Podcast getPodcast() {
            return currentPodcast;
        }

        /**
         * @param podcast The selected podcast to set.
         */
        public void setPodcast(Podcast podcast) {
            this.currentPodcast = podcast;
        }

        /**
         * @return The currently selected episode. Might be <code>null</code>
         * indicating that no selection took place.
         */
        public Episode getEpisode() {
            return currentEpisode;
        }

        /**
         * @param episode The episode to set.
         */
        public void setEpisode(Episode episode) {
            this.currentEpisode = episode;
        }

        /**
         * @return Whether the episode list is sorted old -> new instead of the
         * natural new -> old.
         */
        public boolean isEpisodeOrderReversed() {
            return sortingReversed;
        }

        /**
         * Update the episode list sorting setting.
         *
         * @param reversed Give <code>true</code> reverse natural sorting order.
         */
        public void setEpisodeOrderReversed(boolean reversed) {
            this.sortingReversed = reversed;
        }

        /**
         * @return Whether the filter is set to hide old episodes.
         */
        public boolean isEpisodeFilterEnabled() {
            return filterEnabled;
        }

        /**
         * Update the episode list filter setting.
         *
         * @param active Give <code>true</code> to mark filter enabled.
         */
        public void setEpisodeFilterEnabled(boolean active) {
            this.filterEnabled = active;
        }

        /**
         * @return Whether the fullscreen mode is enabled.
         */
        public boolean isFullscreenEnabled() {
            return fullscreen;
        }

        /**
         * Update the fullscreen mode setting.
         *
         * @param active Give <code>true</code> to mark fullscreen mode enabled.
         */
        public void setFullscreenEnabled(boolean active) {
            this.fullscreen = active;
        }

        /**
         * @return Whether the app is currently in single podcast mode.
         */
        public boolean isSingle() {
            return ContentMode.SINGLE_PODCAST.equals(mode);
        }

        /**
         * @return Whether the app is currently in all podcasts mode.
         */
        public boolean isAll() {
            return ContentMode.ALL_PODCASTS.equals(mode);
        }

        /**
         * @return Whether a specific podcast is selected.
         */
        public boolean isPodcastSet() {
            return currentPodcast != null;
        }

        /**
         * @return Whether a specific episode is selected.
         */
        public boolean isEpisodeSet() {
            return currentEpisode != null;
        }

        /**
         * Completely reset the selection to its initial state.
         */
        public void reset() {
            this.mode = ContentMode.SINGLE_PODCAST;
            resetPodcast();
            resetEpisode();
        }

        /**
         * Reset the podcast selection.
         */
        public void resetPodcast() {
            this.currentPodcast = null;
            this.mode = ContentMode.SINGLE_PODCAST;
        }

        /**
         * Reset the episode selection.
         */
        public void resetEpisode() {
            this.currentEpisode = null;
        }
    }

    @Override
    @SuppressLint("ShowToast")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // On Android 5.0 and later, make sure the app looks nice in "Recent Tasks"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                    null, ContextCompat.getColor(this, R.color.recent_tasks_background)));

        // This will suggest to the Android system, that the volume to be
        // changed for this app (all its activities) is the music stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Set the selection mode member
        selection = ContentSelection.getInstance();
        // Set the view mode member
        view = ViewMode.determineViewMode(getResources());

        // Set the data managers
        podcastManager = PodcastManager.getInstance();
        episodeManager = EpisodeManager.getInstance();
        syncManager = SyncManager.getInstance();

        // Get our preferences and listen to changes
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        // Increment app usage count
        preferences.edit().putInt(APP_USAGE_COUNT_KEY,
                preferences.getInt(APP_USAGE_COUNT_KEY, 0) + 1).apply();

        // Create and configure toast member (not shown here, ignore lint
        // warning). We use only one toast object to avoid stacked notifications
        toast = Toast.makeText(this, null, Toast.LENGTH_SHORT);
        final TextView textView = (TextView) toast.getView().findViewById(android.R.id.message);
        if (textView != null)
            textView.setGravity(Gravity.CENTER);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add generic menu items (help, web site...)
        getMenuInflater().inflate(R.menu.podcatcher, menu);

        // Show/hide review menu item depending on app usage
        final MenuItem reviewMenuItem = menu.findItem(R.id.review_menuitem);
        reviewMenuItem.setVisible(!preferences.getBoolean(REVIEW_MENU_ITEM_CLICKED_KEY, false) &&
                preferences.getInt(APP_USAGE_COUNT_KEY, 0) > APP_USAGE_REVIEW_TRIGGER);

        // Show/hide follow menu item depending on app usage
        final MenuItem followMenuItem = menu.findItem(R.id.follow_menuitem);
        followMenuItem.setVisible(preferences.getBoolean(REVIEW_MENU_ITEM_CLICKED_KEY, false) &&
                !preferences.getBoolean(FOLLOW_MENU_ITEM_CLICKED_KEY, false));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.settings_menuitem:
                    startActivity(new Intent(this, SettingsActivity.class));

                    return true;
                case R.id.review_menuitem:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(BuildConfig.STORE_URL_PREFIX + BuildConfig.APPLICATION_ID)));

                    // Make sure the rate and review menu item is not shown again
                    preferences.edit().putBoolean(REVIEW_MENU_ITEM_CLICKED_KEY, true).apply();

                    return true;
                case R.id.follow_menuitem:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_TWITTER)));

                    // Make sure the follow on twitter menu item is not shown again
                    preferences.edit().putBoolean(FOLLOW_MENU_ITEM_CLICKED_KEY, true).apply();

                    return true;
                case R.id.about_menuitem:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_WEBSITE)));

                    return true;
                case R.id.help_menuitem:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE)));

                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        } catch (ActivityNotFoundException e) {
            // We are in a restricted profile without a browser
            showToast(getString(R.string.no_browser));

            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listener
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // pass, sub-classes might want to use this hook
    }

    /**
     * Gets the fragment for a given tag string id (resolved via app's
     * resources) from the fragment manager.
     *
     * @param tagId Id of the tag string in resources.
     * @return The fragment stored under the given tag or <code>null</code> if
     * not added to the fragment manager.
     */
    protected Fragment findByTagId(int tagId) {
        return getFragmentManager().findFragmentByTag(getString(tagId));
    }

    /**
     * Show a short, centered toast.
     *
     * @param text Toast message text to show.
     */
    protected void showToast(String text) {
        showToast(text, Toast.LENGTH_SHORT);
    }

    /**
     * Show a centered toast.
     *
     * @param text   Toast message text to show.
     * @param length The duration for the toast to show.
     */
    protected void showToast(String text, int length) {
        toast.setText(text);
        toast.setDuration(length);

        toast.show();
    }
}
