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
import com.podcatcher.deluxe.listeners.OnLoadSuggestionListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.tasks.remote.LoadSuggestionsTask;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;

import org.xmlpull.v1.XmlPullParser;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Defines some util methods for tests.
 */
@SuppressWarnings("javadoc")
public class Utils {

    public static final String TEST_STATUS = "Teststatus";

    /**
     * Error code helper for loadAndWait()
     */
    private static PodcastLoadError code = null;

    /**
     * Load and parse podcast given.
     *
     * @param podcast Podcast to process.
     * @return If everything went right, <code>null</code> is returned,
     * otherwise the {@link PodcastLoadError}.
     */
    public static PodcastLoadError loadAndWait(final Podcast podcast) {
        return loadAndWait(podcast, false);
    }

    /**
     * Load and parse podcast given.
     *
     * @param podcast       Podcast to process.
     * @param blockExplicit Whether the task should run with block explicit
     *                      enabled.
     * @return If everything went right, <code>null</code> is returned,
     * otherwise the {@link PodcastLoadError}.
     */
    public static PodcastLoadError loadAndWait(final Podcast podcast, boolean blockExplicit) {
        // Reset code
        code = null;

        // Create task and latch
        final CountDownLatch signal = new CountDownLatch(1);
        final LoadPodcastTask task = new LoadPodcastTask(new OnLoadPodcastListener() {

            @Override
            public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
            }

            @Override
            public void onPodcastLoaded(Podcast podcast) {
                signal.countDown();
            }

            @Override
            public void onPodcastLoadFailed(Podcast podcast, PodcastLoadError errorCode) {
                code = errorCode;

                signal.countDown();
            }
        });

        // Go load podcast
        task.setBlockExplicitEpisodes(blockExplicit);
        task.execute(podcast);

        // Wait for the podcast to load
        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.e(Utils.TEST_STATUS, "Interrupted while waiting for podcast " + podcast.getName());
        }

        return code;
    }

    /**
     * Load example podcasts to run tests on. These will be taken from the list
     * of podcast suggestions.
     *
     * @param context Context to load in. This needs to be able to access the
     *                file system, you might want to use
     *                {@link Instrumentation#getTargetContext()}.
     * @return The list of podcast examples. These only contain the name and URL
     * information, {@link Podcast#parse(XmlPullParser)} has <em>not</em> been called on them.
     */
    public static List<Suggestion> getExamplePodcasts(final Context context) {
        return getExamplePodcasts(context, 0);
    }

    /**
     * Load example podcasts to run tests on. These will be taken from the list
     * of podcast suggestions.
     *
     * @param context Context to load in. This needs to be able to access the
     *                file system, you might want to use
     *                {@link Instrumentation#getTargetContext()}.
     * @param limit   Limit the result to the given number of podcasts randomly
     *                chosen.
     * @return The list of podcast examples. These only contain the name and URL
     * information, {@link Podcast#parse(XmlPullParser)} has <em>not</em> been called on them.
     */
    public static List<Suggestion> getExamplePodcasts(final Context context, final int limit) {
        final CountDownLatch signal = new CountDownLatch(1);
        final List<Suggestion> examples = new ArrayList<>();

        LoadSuggestionsTask task = new LoadSuggestionsTask(context, new OnLoadSuggestionListener() {

            @Override
            public void onSuggestionsLoaded(List<Suggestion> suggestions) {
                Log.d(TEST_STATUS, "Load example podcasts task complete");
                if (limit <= 0)
                    for (Suggestion podcast : suggestions)
                        examples.add(podcast);
                else {
                    int count = 0;

                    while (count++ < limit && !suggestions.isEmpty())
                        examples.add(suggestions.remove(new Random().nextInt(suggestions.size())));
                }

                signal.countDown();
            }

            @Override
            public void onSuggestionsLoadProgress(Progress progress) {
                // Log.d(TEST_STATUS, "Load example podcasts task progress: " +
                // progress);
            }

            @Override
            public void onSuggestionsLoadFailed() {
                Log.d(TEST_STATUS, "Load example podcasts task failed");
                signal.countDown();
            }
        });

        task.execute((Void) null);
        Log.d(TEST_STATUS, "Load example podcasts task started");

        // Wait for the task to finish...
        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.e(TEST_STATUS, e.getMessage(), e);
        }

        return examples;
    }
}
