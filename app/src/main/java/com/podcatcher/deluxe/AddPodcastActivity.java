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

import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.view.fragments.AddPodcastFragment;
import com.podcatcher.deluxe.view.fragments.AddPodcastFragment.AddPodcastDialogListener;
import com.podcatcher.deluxe.view.fragments.AuthorizationFragment;
import com.podcatcher.deluxe.view.fragments.AuthorizationFragment.OnEnterAuthorizationListener;

import static com.podcatcher.deluxe.EpisodeListActivity.PODCAST_URL_KEY;
import static com.podcatcher.deluxe.view.fragments.AuthorizationFragment.USERNAME_PRESET_KEY;

/**
 * Add new podcast(s) activity. This simply shows the add podcast fragment. To
 * preset the feed url edittext, start this activity with an intent that has the
 * feed URL set as its {@link Intent#getData()} return value.
 */
public class AddPodcastActivity extends BaseActivity implements AddPodcastDialogListener,
        OnLoadPodcastListener, OnEnterAuthorizationListener {

    /**
     * Tag to find the add podcast dialog fragment under
     */
    private static final String ADD_PODCAST_DIALOG_TAG = "add_podcast_dialog";
    /**
     * Key to find current load url under
     */
    private static final String LOADING_URL_KEY = "LOADING_URL";
    /**
     * Key to find last user name under
     */
    private static final String LAST_USER_KEY = "LAST_USER_NAME";
    /**
     * The fragment containing the add URL UI
     */
    private AddPodcastFragment addPodcastFragment;
    /**
     * The URL of the podcast we are currently loading (if any)
     */
    private String currentLoadUrl;
    /**
     * The last user name that was put in
     */
    private String lastUserName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Listen to podcast load events to update UI
        podcastManager.addLoadPodcastListener(this);

        // If this is a fresh new activity, create and show the add podcast
        // dialog fragment (tagged for later retrieval)
        if (savedInstanceState == null) {
            this.addPodcastFragment = new AddPodcastFragment();
            // Need to set style, because this activity has no UI
            addPodcastFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialog);

            addPodcastFragment.show(getFragmentManager(), ADD_PODCAST_DIALOG_TAG);
        }
        // Otherwise, if we are coming from a configuration change, we need to
        // get back the fragment handle and we need to know whether there is
        // currently a podcast loading.
        else {
            this.currentLoadUrl = savedInstanceState.getString(LOADING_URL_KEY);
            this.lastUserName = savedInstanceState.getString(LAST_USER_KEY);

            this.addPodcastFragment = (AddPodcastFragment)
                    getFragmentManager().findFragmentByTag(ADD_PODCAST_DIALOG_TAG);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Make sure we know which podcast we are loading (if any)
        outState.putString(LOADING_URL_KEY, currentLoadUrl);
        outState.putString(LAST_USER_KEY, lastUserName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister from data manager
        podcastManager.removeLoadPodcastListener(this);

    }

    @Override
    public void onAddPodcast(String podcastUrl) {
        // Try to load the given online resource
        final Podcast newPodcast = new Podcast(null, podcastUrl);

        // If the podcast is present, select it
        if (podcastManager.contains(newPodcast)) {
            Intent intent = new Intent(this, PodcastActivity.class);
            intent.putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST);
            intent.putExtra(PODCAST_URL_KEY, newPodcast.getUrl());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
            finish();
        }
        // Otherwise try to load it
        else {
            // We need to keep note which podcast we are loading
            currentLoadUrl = newPodcast.getUrl();

            podcastManager.load(newPodcast);
        }
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        if (isCurrentlyLoadingPodcast(podcast))
            addPodcastFragment.showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        if (isCurrentlyLoadingPodcast(podcast)) {
            // Reset current load url
            currentLoadUrl = null;

            // Add podcast and finish the activity
            podcastManager.addPodcast(podcast);
            finish();
        }
    }

    @Override
    public void onPodcastLoadFailed(final Podcast podcast, PodcastLoadError code) {
        if (isCurrentlyLoadingPodcast(podcast)) {
            // Podcasts need authorization
            if (code == PodcastLoadError.AUTH_REQUIRED) {
                // Ask the user for authorization
                AuthorizationFragment authorizationFragment = new AuthorizationFragment();

                if (lastUserName != null) {
                    // Create bundle to make dialog aware of username to pre-set
                    final Bundle args = new Bundle();
                    args.putString(USERNAME_PRESET_KEY, lastUserName);
                    authorizationFragment.setArguments(args);
                }

                authorizationFragment.show(getFragmentManager(), AuthorizationFragment.TAG);
            }
            // Load failed for some other reason
            else {
                // Reset current load url
                currentLoadUrl = null;

                // Show failed UI
                addPodcastFragment.showPodcastLoadFailed(code);
            }
        }
    }

    @Override
    public void onSubmitAuthorization(String username, String password) {
        // We need to keep that in order to pre-fill next time
        lastUserName = username;

        final Podcast newPodcast = new Podcast(null, currentLoadUrl);
        newPodcast.setUsername(username);
        newPodcast.setPassword(password);

        podcastManager.load(newPodcast);
    }

    @Override
    public void onCancelAuthorization() {
        onPodcastLoadFailed(new Podcast(null, currentLoadUrl), PodcastLoadError.ACCESS_DENIED);
    }

    @Override
    public void onShowHelp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE_ADD)));
        } catch (ActivityNotFoundException e) {
            // We are in a restricted profile without a browser, pass
            showToast(getString(R.string.no_browser));
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    private boolean isCurrentlyLoadingPodcast(Podcast podcast) {
        return podcast != null && podcast.equalByUrl(currentLoadUrl);
    }
}
