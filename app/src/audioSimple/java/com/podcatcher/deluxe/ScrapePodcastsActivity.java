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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.podcatcher.deluxe.listeners.OnLoadPodcastListListener;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-UI activity to get podcast list. This can be called from outside the app
 * and will deliver two string lists to {@link #setResult(int, Intent)} in an
 * intent. These list contain the podcast names and urls for all the podcasts
 * available in the app.
 */
public class ScrapePodcastsActivity extends BaseActivity implements OnLoadPodcastListListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        podcastManager.addLoadPodcastListListener(this);

        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null)
            onPodcastListLoaded(podcastList, null);
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList, Uri location) {
        final ArrayList<String> names = new ArrayList<>();
        final ArrayList<String> urls = new ArrayList<>();

        // Add all podcasts to the list
        for (Podcast podcast : podcastList) {
            names.add(podcast.getName());
            urls.add(podcast.getUrl());
        }

        Intent podcasts = new Intent();
        podcasts.putStringArrayListExtra(getString(R.string.podcast_names_key), names);
        podcasts.putStringArrayListExtra(getString(R.string.podcast_urls_key), urls);

        setResult(RESULT_OK, podcasts);

        // Make sure we stop here
        finish();
    }

    @Override
    public void onPodcastListLoadFailed(Uri inputFile, Exception error) {
        onPodcastListLoaded(new ArrayList<Podcast>(), inputFile);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        podcastManager.removeLoadPodcastListListener(this);
    }
}
