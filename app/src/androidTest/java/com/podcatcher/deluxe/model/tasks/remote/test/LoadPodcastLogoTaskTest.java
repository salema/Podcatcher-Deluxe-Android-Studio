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

package com.podcatcher.deluxe.remote.test;

import android.graphics.Bitmap;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.podcatcher.deluxe.listeners.OnLoadPodcastLogoListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastLogoTask;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("javadoc")
public class LoadPodcastLogoTaskTest extends InstrumentationTestCase {

    private CountDownLatch signal = null;

    private List<Podcast> examplePodcasts;

    @Override
    protected void setUp() throws Exception {
        Log.d(Utils.TEST_STATUS, "Set up test \"LoadPodcastLogo\" by loading example podcasts...");

        final Date start = new Date();
        examplePodcasts = Utils.getExamplePodcasts(getInstrumentation().getTargetContext(), 5);

        Log.d(Utils.TEST_STATUS, "Waited " + (new Date().getTime() - start.getTime())
                + "ms for example podcasts...");
    }

    public final void testLoadPodcastLogo() throws Throwable {
        MockPodcastLogoLoader mockLoader = new MockPodcastLogoLoader();

        int size = examplePodcasts.size();
        int index = 0;
        int failed = 0;
        List<String> noLogo = new ArrayList<String>();

        // Actual example Podcast
        Iterator<Podcast> podcasts = examplePodcasts.iterator();
        while (podcasts.hasNext()) {
            Podcast ep = podcasts.next();
            Log.d(Utils.TEST_STATUS, "---- New Podcast (" + ++index + "/" + size + ") ----");
            Log.d(Utils.TEST_STATUS, "Testing \"" + ep + "\"...");

            // Load and parse podcast
            PodcastLoadError errorCode = Utils.loadAndWait(ep);
            if (errorCode != null)
                Log.w(Utils.TEST_STATUS, "Podcast \"" + ep + "\" failed to load: " + errorCode);
            else {
                // Load and check podcast logo
                LoadPodcastLogoTask task = loadAndWait(mockLoader, ep);

                if (mockLoader.failed) {
                    Log.w(Utils.TEST_STATUS, "Logo for podcast " + ep.getName()
                            + " failed to load!");

                    failed++;
                    noLogo.add(ep.getName());
                } else {
                    assertFalse(task.isCancelled());
                    assertFalse(mockLoader.failed);

                    if (mockLoader.result == null || mockLoader.result.getByteCount() == 0)
                        noLogo.add(ep.getName());
                }
            }

            // Discard the complete podcast and its logo because otherwise
            // the memory would fill up quickly...
            podcasts.remove();
        }

        Log.d(Utils.TEST_STATUS, "Tested all example podcast, failed on " + failed);

        if (failed > 0)
            for (String name : noLogo)
                Log.w(Utils.TEST_STATUS, name);
    }

    public final void testLoadWithRelativeLogoPath() {
        Podcast merkel = new Podcast("Merkel",
                "http://www.bundeskanzlerin.de/SiteGlobals/Functions/Webs/BKin/RSSFeed/rssVideoAbo.xml");

        MockPodcastLogoLoader mockLoader = new MockPodcastLogoLoader();
        PodcastLoadError errorCode = Utils.loadAndWait(merkel);
        if (errorCode != null)
            Log.w(Utils.TEST_STATUS, "Podcast \"" + merkel + "\" failed to load: " + errorCode);
        else {
            // Load and check podcast logo
            LoadPodcastLogoTask task = loadAndWait(mockLoader, merkel);

            if (mockLoader.failed) {
                Log.w(Utils.TEST_STATUS, "Logo for podcast " + merkel.getName()
                        + " failed to load!");
            } else {
                assertFalse(task.isCancelled());
                assertFalse(mockLoader.failed);
                assertFalse(mockLoader.result == null);
            }
        }
    }

    private LoadPodcastLogoTask loadAndWait(final MockPodcastLogoLoader mockLoader,
                                            final Podcast podcast) {
        // Create task and latch
        signal = new CountDownLatch(1);
        final LoadPodcastLogoTask task = new LoadPodcastLogoTask(
                getInstrumentation().getTargetContext(), mockLoader);
        task.setMaxAge(1);

        // Go load podcast logo
        final Date start = new Date();
        task.execute(podcast);

        // Wait for the podcast logo to load
        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.e(Utils.TEST_STATUS, "Interrupted while waiting for logo of " + podcast.getName());
        }

        Log.d(Utils.TEST_STATUS, "Waited " + (new Date().getTime() - start.getTime())
                + "ms for Podcast Logo \"" + podcast + "\"...");

        return task;
    }

    private class MockPodcastLogoLoader implements OnLoadPodcastLogoListener {

        protected Bitmap result;
        protected boolean failed;

        @Override
        public void onPodcastLogoLoaded(Podcast podcast) {
            this.result = podcast.getLogo();
            this.failed = false;

            signal.countDown();
        }

        @Override
        public void onPodcastLogoLoadFailed(Podcast podcast) {
            this.failed = true;

            signal.countDown();
        }
    }
}
