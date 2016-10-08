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

package com.podcatcher.deluxe.model.types.test;

import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import android.support.annotation.NonNull;
import android.test.InstrumentationTestCase;

import java.text.ParseException;
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

    private static boolean dateOkay(Date date) {

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
        assertEquals(-1, e.parseDuration("0.0"));
        assertEquals(-1, e.parseDuration("0:0"));
        assertEquals(-1, e.parseDuration("Bla"));
        assertEquals(-1, e.parseDuration("0:00"));
        assertEquals(-1, e.parseDuration("0:00:00"));
        assertEquals(-1, e.parseDuration("00:00:00"));
        assertEquals(-1, e.parseDuration("00:00:00.000"));
        assertEquals(1, e.parseDuration("0:00:01"));
        assertEquals(1, e.parseDuration("0:00:01.000"));
        assertEquals(1, e.parseDuration("0:00:01.499"));
        assertEquals(1, e.parseDuration("00:00:01"));
        assertEquals(1, e.parseDuration("0:01"));
        assertEquals(1, e.parseDuration("1"));
        assertEquals(3600 + 60 + 1, e.parseDuration("1:01:01"));
        assertEquals(12 * 3600 + 59 * 60 + 33, e.parseDuration("12:59:33"));
        assertEquals(12 * 3600 + 59 * 60 + 33, e.parseDuration("12:59:33.000"));
    }

    public final void testUnformatTime() {
        try {
            assertEquals(0, ParserUtils.unformatTime("0"));
            assertEquals(0, ParserUtils.unformatTime("0:0"));
            assertEquals(0, ParserUtils.unformatTime("0:00"));
            assertEquals(0, ParserUtils.unformatTime("0:0.000"));
            assertEquals(0, ParserUtils.unformatTime("0:00"));
            assertEquals(0, ParserUtils.unformatTime("0:00:00"));
            assertEquals(0, ParserUtils.unformatTime("00:00:00"));
            assertEquals(0, ParserUtils.unformatTime("00:00:00.000"));
            assertEquals(1000, ParserUtils.unformatTime("01.000"));
            assertEquals(1000, ParserUtils.unformatTime("0:00:01"));
            assertEquals(1000, ParserUtils.unformatTime("00:00:01"));
            assertEquals(1000, ParserUtils.unformatTime("0:01"));
            assertEquals(1000, ParserUtils.unformatTime("1"));
            assertEquals(1999, ParserUtils.unformatTime("1.999"));
            assertEquals((3600 + 60 + 1) * 1000, ParserUtils.unformatTime("1:01:01"));
            assertEquals((3600 + 60 + 1) * 1000 + 1, ParserUtils.unformatTime("1:01:01.001"));
            assertEquals((12 * 3600 + 59 * 60 + 33) * 1000, ParserUtils.unformatTime("12:59:33"));
        } catch (ParseException pex) {
            assertTrue(false);
        }
    }

    public final void testIsExplicit() {
        Podcast explicit = new Podcast("Sandra", "http://sexnerdsandra.libsyn.com/rss");
        Utils.loadAndWait(explicit);
        assertTrue(explicit.getEpisodes().get(0).isExplicit());

        Podcast clean = new Podcast("SN", "http://feeds.twit.tv/sn.xml");
        Utils.loadAndWait(clean);
        assertFalse(clean.getEpisodes().get(0).isExplicit());
    }

    public final void testNormalizeUrl() {
        Podcast dummy = new Podcast(null, null);

        assertEquals("http://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3",
                new EpisodeForTesting(dummy, 0).normalizeUrl("www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3"));
        assertEquals("http://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3",
                new EpisodeForTesting(dummy, 0).normalizeUrl("http://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3"));
        assertEquals("https://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3",
                new EpisodeForTesting(dummy, 0).normalizeUrl("https://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3"));
        assertEquals("http://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3",
                new EpisodeForTesting(dummy, 0).normalizeUrl("file://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/sciencefriday/scifri201408221.mp3"));
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
        public int parseDuration(@NonNull String text) {
            return super.parseDuration(text);
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public String normalizeUrl(String spec) {
            return super.normalizeUrl(spec);
        }
    }
}
