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

package com.podcatcher.deluxe.model.test;

import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;

import android.support.annotation.NonNull;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test podcast suggestions at podcatcher-deluxe.com
 */
@SuppressWarnings("javadoc")
public class SuggestionTester extends InstrumentationTestCase implements OnLoadPodcastListener {

    private static final String TAG = "TESTER";

    /* We need a way to wait for all podcasts to load */
    private CountDownLatch signal;

    /**
     * Age of latest podcast episode to trigger warning. (Give -1 to disable warning.)
     */
    private static final int WARN_RECENT_DAYS = 365;

    public final void testSuggestions() {
        final List<Suggestion> suggestions = Utils.getExamplePodcasts(getInstrumentation().getTargetContext());
        Log.i(TAG, "List of existing suggestions loaded: " + suggestions.size() + " podcasts");

        final int threadCount = Runtime.getRuntime().availableProcessors() + 2;
        final Executor executor = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "LoadPodcastTask #" + count.getAndIncrement());
            }
        });

        int feedCount = 0;
        for (Suggestion suggestion : suggestions)
            feedCount += suggestion.getFeeds().size();

        this.signal = new CountDownLatch(feedCount);

        for (Suggestion suggestion : suggestions)
            for (Map.Entry<String, String> entry : suggestion.getFeeds().entrySet()) {
                final String name = suggestion.getName() + " (" + entry.getKey() + ")";
                final Podcast podcast = new Podcast(name, entry.getValue());
                new LoadPodcastTask(this).executeOnExecutor(executor, podcast);
            }

        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for podcast suggestions");
        }
        Log.i(TAG, "Done testing existing suggestions");
    }


    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // pass
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        Log.i(TAG, "Podcast " + podcast.getName() + " loaded");
        signal.countDown();

        if (podcast.getEpisodeCount() > 0) {
            final List<Episode> episodes = podcast.getEpisodes();
            Collections.sort(episodes);

            long ageInDays = 0;
            // Negative age might happen, just invert those (too far in the future is also strange...)
            if (episodes.get(0).getPubDate() != null)
                ageInDays = Math.abs((new Date().getTime() - episodes.get(0).getPubDate().getTime()) / TimeUnit.DAYS.toMillis(1));

            if (WARN_RECENT_DAYS > 0 && ageInDays > WARN_RECENT_DAYS)
                Log.w(TAG, "Podcast " + podcast.getName() + " latest episode is " + ageInDays + " days old.");
        } else
            Log.w(TAG, "Podcast " + podcast.getName() + " has no episodes.");
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast, LoadPodcastTask.PodcastLoadError code) {
        Log.w(TAG, "Podcast " + podcast.getName() + " failed with code: " + code);
        signal.countDown();
    }
}
