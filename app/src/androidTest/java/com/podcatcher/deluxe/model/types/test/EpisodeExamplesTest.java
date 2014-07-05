package com.podcatcher.deluxe.model.types.test;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.Date;

@SuppressWarnings("javadoc")
public class EpisodeExamplesTest extends SuggestionsAsExamplesTest {

    @Override
    protected void setUp() throws Exception {
        sampleSize = 25;

        super.setUp();
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
        for (Podcast podcast : examplePodcasts)
            for (Episode episode : podcast.getEpisodes())
                for (Podcast otherPodcast : examplePodcasts)
                    for (Episode otherEpisode : otherPodcast.getEpisodes())
                        assertEquals(
                                "LHS: " + episode.getName() + " RHS: " + otherEpisode.getName(),
                                episode.equals(otherEpisode),
                                episode.compareTo(otherEpisode) == 0);

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
                assertNotNull(episode.getMediaUrl());
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
        Podcast extra = new Podcast("Skip Heinzig", "http://skipheitzig.com/podcast/tv");
        Utils.loadAndWait(extra);
        Podcast wtf = new Podcast("WTFPod", "http://www.wtfpod.com/podcast/rss");
        Utils.loadAndWait(wtf);
        Podcast ru = new Podcast("Radio Underground",
                "http://feeds.feedburner.com/radiounderground");
        Utils.loadAndWait(ru);
        Podcast know = new Podcast("To the best of our knowledge",
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

    @SmallTest
    public final void testGetDuration() {
        for (Podcast podcast : examplePodcasts)
            for (Episode episode : podcast.getEpisodes()) {
                final String episodeName = "Episode " + episode.getName() + " in Podcast "
                        + episode.getPodcast().getName();

                assertEquals(episodeName, episode.getDuration() > 0,
                        episode.getDurationString() != null);
                assertFalse(episodeName, episode.getDuration() == 0);
            }
    }
}
