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

package com.podcatcher.deluxe.model.types.test;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Base64;

import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Podcast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@SuppressWarnings("javadoc")
public class PodcastTest extends InstrumentationTestCase {

    public final void testEquals() {
        assertFalse(new Podcast(null, null).equals(new Podcast(null, null)));

        Podcast tal = new Podcast(null,
                "http://feeds.thisamericanlife.org/talpodcast");
        assertTrue(tal.equals(tal));
        assertFalse(tal.equals(getInstrumentation()));

        Podcast tal2 = new Podcast(null,
                "http://feeds.thisamericanlife.org/talpodcast");
        assertTrue(tal.equals(tal2));

        Podcast tal3 = new Podcast(null,
                "HTTP://feeds.thisamericanlife.org/talpodcast/");
        assertTrue(tal.equals(tal3));
        assertTrue(tal.equalByUrl(tal3.getUrl()));
        assertFalse(tal.getUrl().equals("HTTP://feeds.thisamericanlife.org/talpodcast/"));
        assertFalse(tal.equalByUrl(null));
        assertFalse(tal.equalByUrl(""));
        assertFalse(tal.equalByUrl("https://feeds.thisamericanlife.org/talpodcast"));
        assertFalse(tal.equalByUrl("https://feeds.thisamericanlife.org/podcast"));
    }

    public final void testHashCode() {
        assertTrue(new Podcast(null, null).hashCode() != 0);
    }

    public final void testCompareTo() {
        assertTrue(new Podcast(null, null).compareTo(new Podcast(null, null)) == 0);
        assertEquals("Bla".compareToIgnoreCase("ABZ"),
                new Podcast("Bla", null).compareTo(new Podcast("ABZ", null)));
        assertEquals("Bla".compareToIgnoreCase("bla"),
                new Podcast("Bla", null).compareTo(new Podcast("bla", null)));
        assertEquals("ABC".compareToIgnoreCase("ABZ"),
                new Podcast("ABC", null).compareTo(new Podcast("ABZ", null)));
        assertEquals("ABC".compareToIgnoreCase("ABC"),
                new Podcast("ABC", null).compareTo(new Podcast("ABC", null)));
    }

    public final void testGetName() throws XmlPullParserException,
            IOException {
        String name = null;
        Podcast podcast = new Podcast(name, null);
        assertEquals(name, podcast.getName());

        name = "";
        podcast = new Podcast(name, null);
        assertEquals(name, podcast.getName());

        name = "Test";
        podcast = new Podcast(name, null);
        assertEquals(name, podcast.getName());

        Podcast tal = new Podcast(null,
                "http://feeds.thisamericanlife.org/talpodcast");
        assertNull(tal.getName());
        Utils.loadAndWait(tal);
        assertTrue(tal.getName().length() > 10);

        Podcast tal2 = new Podcast("",
                "http://feeds.thisamericanlife.org/talpodcast");
        Utils.loadAndWait(tal2);
        assertEquals(tal2.getName(), tal.getName());

        Podcast tal3 = new Podcast("    ",
                "http://feeds.thisamericanlife.org/talpodcast");
        Utils.loadAndWait(tal3);
        assertEquals(tal3.getName(), tal.getName());
    }

    public final void testToString() {
        String name = null;
        Podcast podcast = new Podcast(name, null);
        assertNotNull(podcast.toString());
    }

    public final void testGetEpisodeNumber() {
        assertEquals(0, new Podcast(null, null).getEpisodeCount());

        Podcast tal = new Podcast("TAL",
                "http://feeds.thisamericanlife.org/talpodcast");
        assertEquals(0, tal.getEpisodeCount());
        Utils.loadAndWait(tal);
        assertEquals(1, tal.getEpisodeCount());
    }

    public final void testGetEpisodes() {
        assertNotNull(new Podcast(null, null).getEpisodes());

        Podcast tal = new Podcast("TAL",
                "http://feeds.thisamericanlife.org/talpodcast");
        assertTrue(tal.getEpisodes().isEmpty());
        Utils.loadAndWait(tal);
        assertFalse(tal.getEpisodes().isEmpty());

        Podcast merkel = new Podcast("Merkel",
                "http://www.bundeskanzlerin.de/SiteGlobals/Functions/Webs/BKin/RSSFeed/rssVideoAbo.xml");
        assertTrue(merkel.getEpisodes().isEmpty());
        Utils.loadAndWait(merkel);
        assertFalse(merkel.getEpisodes().isEmpty());
    }

    public final void testGetLogoUrl() {
        assertNull(new Podcast(null, null).getLogoUrl());

        Podcast tal = new Podcast("TAL",
                "http://feeds.thisamericanlife.org/talpodcast");
        assertNull(tal.getLogoUrl());
        Utils.loadAndWait(tal);
        assertNotNull(tal.getLogoUrl());
    }

    public final void testLastLoaded() {
        assertNull(new Podcast(null, null).getLastLoaded());

        Podcast tal = new Podcast("TAL",
                "http://feeds.thisamericanlife.org/talpodcast");
        assertNull(tal.getLastLoaded());
        Utils.loadAndWait(tal);
        assertNotNull(tal.getLastLoaded());
    }

    @MediumTest
    public final void testIsExplicit() {
        assertFalse(new Podcast(null, null).isExplicit());

        Podcast explicit = new Podcast("NoSleep", "http://nosleeppodcast.libsyn.com/rss");
        assertFalse(explicit.isExplicit());
        Utils.loadAndWait(explicit);
        assertTrue(explicit.isExplicit());

        Podcast clean = new Podcast("TAL", "http://feeds.thisamericanlife.org/talpodcast");
        assertFalse(clean.isExplicit());
        Utils.loadAndWait(clean);
        assertFalse(clean.isExplicit());
    }

    public final void testGetAuth() {
        Podcast podcast = new Podcast(null, null);
        assertNull(podcast.getAuthorization());
        podcast.setUsername("kevin");
        assertNull(podcast.getAuthorization());

        podcast.setUsername(null);
        podcast.setPassword("monkey");
        assertNull(podcast.getAuthorization());

        podcast.setUsername("kevin");
        assertNotNull(podcast.getAuthorization());
        assertEquals(podcast.getAuthorization(),
                "Basic " + Base64.encodeToString("kevin:monkey".getBytes(),
                        Base64.NO_WRAP)
        );
    }

    public final void testToAbsoluteUrl() {
        String url = "http://some-server.com/feeds/podcast.xml";
        PodcastDummy dummy = new PodcastDummy(null, url);

        assertEquals(null, dummy.toAbsoluteUrl(null));
        assertEquals("", dummy.toAbsoluteUrl(""));
        assertEquals(url, dummy.toAbsoluteUrl(url));
        assertEquals("http://some-server.com/feeds/blödsinn",
                dummy.toAbsoluteUrl("blödsinn"));
        assertEquals("http://some-server.com/bla/image.png",
                dummy.toAbsoluteUrl("/bla/image.png"));
        assertEquals("http://some-server.com/feeds/bla/image.png",
                dummy.toAbsoluteUrl("bla/image.png"));
    }

    public final void testNormalizeUrl() {
        assertNull(new Podcast(null, null).getUrl());
        assertEquals("", new Podcast(null, "").getUrl());
        assertEquals("nothing-serious", new Podcast(null, "nothing-serious").getUrl());
        assertEquals("htp://mygreatpodcast.Test.com",
                new Podcast(null, "htp://mygreatpodcast.Test.com").getUrl());

        assertEquals("http://www.npr.org/rss/podcast.php?id=510289",
                new Podcast(null, "http://www.npr.org/rss/podcast.php?id=510289").getUrl());

        assertEquals("http://mygreatpodcast.test.com/",
                new Podcast(null, "http://mygreatpodcast.Test.com").getUrl());
        assertEquals("http://mygreatpodcast.test.com/",
                new Podcast(null, "feed://mygreatpodcast.Test.com").getUrl());
        assertEquals("http://mygreatpodcast.test.com/",
                new Podcast(null, "itPC://mygreatpodcast.Test.com").getUrl());
        assertEquals("http://mygreatpodcast.test.com/",
                new Podcast(null, "Itms://mygreatpodcast.Test.com").getUrl());
        assertEquals("http://mygreatpodcast.test.com/?format=rss",
                new Podcast(null, "http://mygreatpodcast.Test.com/?format=rss").getUrl());
        assertEquals("http://mygreatpodcast.test.com/test?format=rss",
                new Podcast(null, "http://mygreatpodcast.Test.com/test?format=rss#foo").getUrl());
        assertEquals("http://feeds.feedburner.com/TheTest",
                new Podcast(null, " http://feeds.feedburner.com/TheTest ").getUrl());
        assertEquals("http://feeds.feedburner.com/TheTest",
                new Podcast(null, "htTP://feeds2.FeedBurner.com:80/TheTest/").getUrl());
        assertEquals("http://feeds.feedburner.com:83/TheTest",
                new Podcast(null, "htTP://feeds2.FeedBurner.com:83/TheTest?format=xml").getUrl());
        assertEquals("https://feeds.feedburner.com:83/TheTest",
                new Podcast(null, "htTPs://feeds2.FeedBurner.com:83/TheTest?format=xml").getUrl());
        assertEquals("https://feeds.feedburner.com/TheTest",
                new Podcast(null, "htTPs://feeds2.FeedBurner.com:443/TheTest?format=xml").getUrl());

        assertEquals("http://feeds.feedburner.com/TestPodcast",
                new Podcast(null, "FB:TestPodcast?format=xml").getUrl());

        Podcast test = new Podcast(null, "http://kevin@feeds.feedburner.com/TestPodcast");
        assertEquals("kevin", test.getUsername());
        assertNull(test.getPassword());
        test = new Podcast(null, "http://kevin:@feeds.feedburner.com/TestPodcast");
        assertEquals("kevin", test.getUsername());
        assertNull(test.getPassword());
        test = new Podcast(null, "http://feeds.feedburner.com/TestPodcast");
        assertNull(test.getUsername());
        assertNull(test.getPassword());
        Podcast test2 = new Podcast(null, "http://kevin:test@feeds.feedburner.com/TestPodcast");
        assertEquals("kevin", test2.getUsername());
        assertEquals("test", test2.getPassword());
        assertEquals(test, test2);
        assertTrue(test.equalByUrl(test2.getUrl()));
        assertTrue(test2.equalByUrl(test.getUrl()));
    }

    private class PodcastDummy extends Podcast {
        public PodcastDummy(String name, String url) {
            super(name, url);
        }

        public String toAbsoluteUrl(String relativeUrl) {
            return super.toAbsoluteUrl(relativeUrl);
        }
    }
}
