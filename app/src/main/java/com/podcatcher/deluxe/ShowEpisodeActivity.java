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

import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.fragments.EpisodeFragment;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

/**
 * Activity to show only the episode and possibly the player. Used in small
 * portrait view mode only.
 */
public class ShowEpisodeActivity extends EpisodeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In large or landscape layouts we do not need this activity at
        // all, so finish it. Also there is the case where the Android system
        // recreates this activity after the app has been killed and the
        // activity would show up empty because there is no episode selected.
        if (!view.isSmallPortrait() || !selection.isEpisodeSet())
            finish();
        else {
            // 1. Set the content view
            setContentView(R.layout.main);
            // 2. Set, find, create the fragments
            findFragments();
            // During initial setup, plug in the details fragment.
            if (savedInstanceState == null && episodeFragment == null) {
                episodeFragment = new EpisodeFragment();
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, episodeFragment,
                                getString(R.string.episode_fragment_tag))
                        .commit();
            }

            // 3. Register the listeners needed to function as a controller
            registerListeners();

            // 4. Set episode in fragment UI
            onEpisodeSelected(selection.getEpisode());
        }
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        super.onEpisodeSelected(selectedEpisode);

        episodeFragment.setEpisode(selectedEpisode);
    }

    @Override
    public void onEpisodeListSwipeToRefresh() {
        // Nothing to do here
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Deselect episode (prevents the activity from being immediately re-opened)
            selection.resetEpisode();

            NavUtils.navigateUpFromSameTask(this);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);

            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Deselect episode (prevents the activity from being immediately re-opened)
        selection.resetEpisode();

        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }

    @Override
    protected void updateActionBar() {
        getActionBar().setTitle(R.string.app_name);
        getActionBar().setSubtitle(null);

        // Enable navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
