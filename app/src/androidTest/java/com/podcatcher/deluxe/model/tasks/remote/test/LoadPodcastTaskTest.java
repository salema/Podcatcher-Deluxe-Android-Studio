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

package com.podcatcher.deluxe.model.tasks.remote.test;

import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;

import android.test.InstrumentationTestCase;
import android.util.Log;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("javadoc")
public class LoadPodcastTaskTest extends InstrumentationTestCase {

    private CountDownLatch signal = null;

    private List<Suggestion> examplePodcasts;

    @Override
    protected void setUp() throws Exception {
        Log.d(Utils.TEST_STATUS,
                "Set up test \"LoadPodcast\" by loading example podcasts...");

        final Date start = new Date();
        examplePodcasts =
                Utils.getExamplePodcasts(getInstrumentation().getTargetContext(), 10);

        Log.d(Utils.TEST_STATUS, "Waited " + (new Date().getTime() -
                start.getTime())
                + "ms for example podcasts...");
    }

    public final void testLoadExamplePodcasts() {
        final MockPodcastLoader mockLoader = new MockPodcastLoader();

        int size = examplePodcasts.size();
        int index = 0;
        int failed = 0;

        Log.d(Utils.TEST_STATUS, "Testing " + size + " example podcasts");

        // Actual example podcasts
        Iterator<Suggestion> podcasts = examplePodcasts.iterator();
        while (podcasts.hasNext()) {
            Podcast ep = podcasts.next();
            Log.d(Utils.TEST_STATUS, "---- New Podcast (" + ++index + "/" + size +
                    ") ----");
            Log.d(Utils.TEST_STATUS, "Testing \"" + ep + "\"...");
            LoadPodcastTask task = loadAndWait(mockLoader, new Podcast(ep.getName(),
                    ep.getUrl()), false);

            if (mockLoader.failed) {
                Log.w(Utils.TEST_STATUS, "Podcast " + ep.getName() + " failed!");
                assertNull(mockLoader.result.getLastLoaded());

                failed++;
            } else {
                assertFalse(task.isCancelled());
                assertNotNull(mockLoader.result);
                assertNotNull(mockLoader.result.getLastLoaded());

                if (mockLoader.result.getEpisodes().isEmpty())
                    Log.w(Utils.TEST_STATUS, "Podcast " + ep.getName() + " has not episodes!");
                else
                    Log.d(Utils.TEST_STATUS, "Tested " + mockLoader.result + " - okay...");
            }

            // Discard the complete podcast because otherwise
            // the memory would fill up quickly...
            podcasts.remove();
        }

        Log.d(Utils.TEST_STATUS, "Tested all example podcast, failed on " +
                failed);
    }

    public final void testLoadDummyPodcasts() {
        final MockPodcastLoader mockLoader = new MockPodcastLoader();

        // null
        loadAndWait(mockLoader, null, false);
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result);
        assertEquals(mockLoader.code, PodcastLoadError.UNKNOWN);

        // null URL
        loadAndWait(mockLoader, new Podcast(null, null), false);
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.NOT_REACHABLE);

        // bad URL
        loadAndWait(mockLoader, new Podcast("Mist", "http://bla"), false);
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.NOT_REACHABLE);

        // no podcast feed URL
        loadAndWait(mockLoader, new Podcast("Google", "http://www.google.com"),
                false);
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.NOT_PARSABLE);

        // Auth required
        loadAndWait(mockLoader, new Podcast("SGU",
                        "https://www.theskepticsguide.org/premium"),
                false
        );
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.AUTH_REQUIRED);

        // No title given
        loadAndWait(mockLoader, new Podcast("MSS",
                        "http://www.mormonsundayschool.org/feed/"),
                false
        );
        assertFalse(mockLoader.failed);
        Podcast p = mockLoader.result;
        assertNotNull(p);
        assertFalse(p.getEpisodes().isEmpty());
        assertNotNull(p.getLastLoaded());
        assertFalse(p.getName() == null || p.getName().isEmpty());
    }

    public final void testLoadWithBlockExplicitEpisodes() {
        final MockPodcastLoader mockLoader = new MockPodcastLoader();

        Podcast explicit = new Podcast("NoSleep", "http://nosleeppodcast.libsyn.com/rss");
        loadAndWait(mockLoader, explicit, false);
        assertTrue(explicit.isExplicit());
        assertTrue(explicit.getEpisodeCount() > 0);
        final int episodeCountAll = explicit.getEpisodeCount();
        assertTrue(explicit.getEpisodes().get(0).isExplicit());
        assertFalse(mockLoader.failed);
        Podcast explicit2 = new Podcast("NoSleep", "http://nosleeppodcast.libsyn.com/rss");
        loadAndWait(mockLoader, explicit2, true);
        assertTrue(explicit2.isExplicit());
        assertTrue(explicit2.getEpisodeCount() < episodeCountAll);
        //assertTrue(mockLoader.failed);
        //assertEquals(mockLoader.code, PodcastLoadError.EXPLICIT_BLOCKED);

        Podcast clean = new Podcast("SN", "http://feeds.twit.tv/sn.xml");
        loadAndWait(mockLoader, clean, false);
        assertFalse(clean.isExplicit());
        assertTrue(clean.getEpisodeCount() > 0);
        assertFalse(clean.getEpisodes().get(0).isExplicit());
        assertFalse(mockLoader.failed);
        Podcast clean2 = new Podcast("SN", "http://feeds.twit.tv/sn.xml");
        loadAndWait(mockLoader, clean2, true);
        assertFalse(clean2.isExplicit());
        assertTrue(clean2.getEpisodeCount() > 0);
        assertFalse(clean2.getEpisodes().get(0).isExplicit());
        assertFalse(mockLoader.failed);
    }

    private LoadPodcastTask loadAndWait(final MockPodcastLoader mockLoader, final Podcast podcast,
                                        boolean blockExplicit) {
        // Create task and latch
        final LoadPodcastTask task = new LoadPodcastTask(mockLoader);
        task.setBlockExplicitEpisodes(blockExplicit);
        signal = new CountDownLatch(1);

        // Go load podcast
        final Date start = new Date();
        task.execute(podcast);

        // Wait for the podcast to load
        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.e(Utils.TEST_STATUS, "Interrupted while waiting for podcast " + podcast.getName());
        }

        Log.d(Utils.TEST_STATUS, "Waited " + (new Date().getTime() - start.getTime())
                + "ms for Podcast \"" + podcast + "\"...");

        return task;
    }

    private class MockPodcastLoader implements OnLoadPodcastListener {

        Podcast result;
        boolean failed;
        PodcastLoadError code;

        @Override
        public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        }

        @Override
        public void onPodcastMoved(Podcast podcast, String newUrl) {
            this.result = podcast;
            this.failed = true;
            this.code = null;

            Log.w(Utils.TEST_STATUS, "Podcast " + podcast.getName() + " needs to move to " + newUrl);
            signal.countDown();
        }

        @Override
        public void onPodcastLoaded(Podcast podcast) {
            this.result = podcast;
            this.failed = false;
            this.code = null;

            signal.countDown();
        }

        @Override
        public void onPodcastLoadFailed(Podcast podcast, PodcastLoadError code) {
            this.result = podcast;
            this.failed = true;
            this.code = code;

            signal.countDown();
        }
    }
}
