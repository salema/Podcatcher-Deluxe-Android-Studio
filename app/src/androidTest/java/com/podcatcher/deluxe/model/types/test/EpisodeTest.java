package com.podcatcher.deluxe.model.types.test;

import android.test.InstrumentationTestCase;

import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.Date;

@SuppressWarnings("javadoc")
public class EpisodeTest extends InstrumentationTestCase {

    public final void testCompareTo() {
        Date one = new Date(100);
        Date same = new Date(100);
        Date other = new Date(101);

        Podcast dummy = new Podcast(null, null);
        EpisodeForTesting first = new EpisodeForTesting(dummy, 1);
        first.setPubDate(one);
        EpisodeForTesting second = new EpisodeForTesting(dummy, 2);

        assertTrue(first.compareTo(second) > 0);
        assertTrue(second.compareTo(first) < 0);

        second.setPubDate(same);
        EpisodeForTesting third = new EpisodeForTesting(dummy, 1);
        third.setPubDate(other);

        assertEquals(first.getPositionInPodcast() - second.getPositionInPodcast(),
                first.compareTo(second));
        assertEquals(second.getPositionInPodcast() - first.getPositionInPodcast(),
                second.compareTo(first));
        assertEquals(one.compareTo(other), -first.compareTo(third));
        assertEquals(other.compareTo(one), -third.compareTo(first));
    }

    public final void testParsePubDate() {
        Podcast dummy = new Podcast(null, null);
        EpisodeForTesting e = new EpisodeForTesting(dummy, 1);

        assertTrue(dateOkay(e.parsePubDate("Sun, 17 Nov 2013 00:00:00 -0600")));
        assertTrue(dateOkay(e.parsePubDate("Sun, 3 Nov 2013 00:00:00 -0500")));
        assertTrue(dateOkay(e.parsePubDate("Sun, 10 Nov 2013 00:00:00 -0600")));
    }

    private boolean dateOkay(Date date) {

        return date != null &&
                // No more then 10 years back
                date.after(new Date(new Date().getTime() - 1000l * 60 * 60 * 24 * 365 * 10)) &&
                // No more then one week into the future
                date.before(new Date(new Date().getTime() + 1000 * 60 * 60 * 24 * 7));
    }

    public final void testParseDuration() {
        Podcast dummy = new Podcast(null, null);
        EpisodeForTesting e = new EpisodeForTesting(dummy, 1);
        assertEquals(-1, e.parseDuration(null));
        assertEquals(-1, e.parseDuration(""));
        assertEquals(-1, e.parseDuration("   "));
        assertEquals(-1, e.parseDuration("0"));
        assertEquals(-1, e.parseDuration("0:0"));
        assertEquals(-1, e.parseDuration("Bla"));
        assertEquals(-1, e.parseDuration("0:00"));
        assertEquals(-1, e.parseDuration("0:00:00"));
        assertEquals(-1, e.parseDuration("00:00:00"));
        assertEquals(1, e.parseDuration("0:00:01"));
        assertEquals(1, e.parseDuration("00:00:01"));
        assertEquals(1, e.parseDuration("0:01"));
        assertEquals(1, e.parseDuration("1"));
        assertEquals(1 * 3600 + 1 * 60 + 1, e.parseDuration("1:01:01"));
        assertEquals(12 * 3600 + 59 * 60 + 33, e.parseDuration("12:59:33"));
    }

    public final void testGetDuration() {
        Podcast dummy = new Podcast(null, null);
        EpisodeForTesting e = new EpisodeForTesting(dummy, 1);
        e.setDuration(0);
        assertEquals(null, e.getDurationString());
        e.setDuration(-1);
        assertEquals(null, e.getDurationString());
        e.setDuration(1);
        assertEquals("0:01", e.getDurationString());
        e.setDuration(60);
        assertEquals("1:00", e.getDurationString());
        e.setDuration(1 * 3600 + 1 * 60 + 1);
        assertEquals("1:01:01", e.getDurationString());
        e.setDuration(12 * 3600 + 59 * 60 + 33);
        assertEquals("12:59:33", e.getDurationString());
    }

    public final void testIsExplicit() {
        Podcast wtf = new Podcast("Adam Carolla",
                "http://feeds.feedburner.com/TheAdamCarollaPodcast");
        Utils.loadAndWait(wtf);
        assertTrue(wtf.getEpisodes().get(0).isExplicit());

        Podcast tal = new Podcast("TAL", "http://feeds.thisamericanlife.org/talpodcast");
        Utils.loadAndWait(tal);
        assertFalse(tal.getEpisodes().get(0).isExplicit());
    }

    class EpisodeForTesting extends Episode {

        public EpisodeForTesting(Podcast podcast, int index) {
            super(podcast, index);
        }

        public void setPubDate(Date date) {
            this.pubDate = date;
        }

        public Date parsePubDate(String dateString) {
            return parseDate(dateString);
        }

        @Override
        public int parseDuration(String text) {
            return super.parseDuration(text);
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }
    }
}
