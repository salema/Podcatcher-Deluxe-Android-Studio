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

package com.podcatcher.deluxe.model.types.test;

import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Suggestion;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.Date;

@SuppressWarnings("javadoc")
public class EpisodeExamplesTest extends SuggestionsAsExamplesTest {

    @Override
    protected void setUp() throws Exception {
        sampleSize = 10;

        super.setUp();

        for (Podcast podcast : examplePodcasts)
            Log.d(Utils.TEST_STATUS, "Selected podcast " + podcast.getName() + " (" + podcast.getEpisodeCount() + " episodes)");
    }

    @SmallTest
    public final void testEquals() {
        for (Podcast podcast : examplePodcasts) {
            Episode first = null;
            for (Episode episode : podcast.getEpisodes()) {
                assertFalse(episode.equals(null));
                assertTrue(episode.equals(episode));
                assertFalse(episode.equals(new Object()));
                assertFalse(episode.equals(new Podcast(null, null)));
                assertFalse(episode.equals(podcast));

                if (podcast.getEpisodes().indexOf(episode) == 0)
                    first = episode;
                else if (first != null)
                    assertFalse(first.equals(episode));
            }
        }
    }

    @SmallTest
    public final void testHashCode() {
        for (Podcast podcast : examplePodcasts) {

            Episode first = null;
            for (Episode episode : podcast.getEpisodes()) {
                assertTrue(episode.hashCode() != 0);

                if (podcast.getEpisodes().indexOf(episode) == 0)
                    first = episode;
                else if (first != null)
                    assertFalse(first.hashCode() == episode.hashCode());
            }
        }
    }

    @LargeTest
    public final void testCompareTo() {
        for (Podcast podcast : examplePodcasts) {
            Log.d(Utils.TEST_STATUS, "Starting compareTo for podcast " + podcast.getName());

            for (Episode episode : podcast.getEpisodes())
                for (Podcast otherPodcast : examplePodcasts)
                    for (Episode otherEpisode : otherPodcast.getEpisodes())
                        assertEquals(
                                "LHS: " + episode.getName() + " RHS: " + otherEpisode.getName(),
                                episode.equals(otherEpisode),
                                episode.compareTo(otherEpisode) == 0);
        }

        // "LHS: " + episode.getName() + "/"
        // + episode.getPodcast().getName() + "/"
        // + episode.getPubDate()
        // + " RHS: " + otherEpisode.getName() + "/"
        // + otherEpisode.getPodcast().getName() + "/"
        // + otherEpisode.getPubDate(),
    }

    @SmallTest
    public final void testGetName() {
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                assertNotNull(episode.getName());
                assertTrue(episode.getName().length() > 0);
                assertFalse(episode.getName().contains("\n"));
                assertFalse(episode.getName().contains("\r"));
                assertFalse(episode.getName().contains("\r\n"));
            }
        }
    }

    @SmallTest
    public final void testGetMediaUrl() {
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                String messageStart = "Episode " + episode.getName() + "(" + episode.getPodcast().getName() + ")";

                assertNotNull(messageStart + " has null media url", episode.getMediaUrl());
                assertTrue(messageStart + " has bad media url: " + episode.getMediaUrl(), episode.getMediaUrl().startsWith("http"));
            }
        }
    }

    @SmallTest
    public final void testGetDuration() {
        for (Podcast podcast : examplePodcasts)
            for (Episode episode : podcast.getEpisodes()) {
                final String episodeName = "Episode " + episode.getName() + " in Podcast "
                        + episode.getPodcast().getName();

                assertFalse(episodeName, episode.getDuration() == 0);
            }
    }

    @SmallTest
    public final void testGetMediaLength() {
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                String messageStart = "Episode " + episode.getName() + "(" + episode.getPodcast().getName() + ")";

                assertTrue(messageStart + " has no media file size information", episode.getFileSize() > 0);
            }
        }
    }

    @SmallTest
    public final void testGetMediaType() {
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                String messageStart = "Episode " + episode.getName() + "(" + episode.getPodcast().getName() + ")";

                assertNotNull(messageStart + " has null media type", episode.getMediaType());
            }
        }
    }

    @SmallTest
    public final void testGetPodcastName() {
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                assertEquals(episode.getPodcast().getName(), podcast.getName());
            }
        }
    }

    @SmallTest
    public final void testGetPubDate() {
        Suggestion extra = new Suggestion("Skip Heinzig", "http://skipheitzig.com/podcast/tv");
        Utils.loadAndWait(extra);
        Suggestion wtf = new Suggestion("WTFPod", "http://www.wtfpod.com/podcast/rss");
        Utils.loadAndWait(wtf);
        Suggestion ru = new Suggestion("Radio Underground",
                "http://feeds.feedburner.com/radiounderground");
        Utils.loadAndWait(ru);
        Suggestion know = new Suggestion("To the best of our knowledge",
                "http://ttbook.org/book/radio/rss/feed");
        Utils.loadAndWait(know);

        examplePodcasts.add(extra);
        examplePodcasts.add(wtf);
        examplePodcasts.add(ru);
        examplePodcasts.add(know);

        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                final String episodeName = "Episode " + episode.getName() + " in Podcast "
                        + episode.getPodcast().getName();

                assertNotNull(episodeName, episode.getPubDate());
                // No more then 10 years back
                assertTrue(episodeName, episode.getPubDate().after(
                        new Date(new Date().getTime() - 1000l * 60 * 60 * 24 * 365 * 10)));
                // No more then one week into the future
                assertTrue(episodeName, episode.getPubDate().before(
                        new Date(new Date().getTime() + 1000 * 60 * 60 * 24 * 7)));
            }
        }
    }
}
