package com.podcatcher.deluxe.model.test;

import android.test.InstrumentationTestCase;

import java.io.File;

import static com.podcatcher.deluxe.model.EpisodeDownloadManager.sanitizeAsFilePath;

@SuppressWarnings("javadoc")
public class EpisodeDownloadManagerTest extends InstrumentationTestCase {

    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'#!,&";

    public final void testSanitize() {
        // assertEquals("", sanitizeAsFilePath("", "", ""));

        final char s = File.separatorChar;
        final String url = "http://www.example.com/podcast/";

        assertEquals("a" + s + "b", sanitizeAsFilePath("a", "b", ""));
        assertEquals("a" + s + "b", sanitizeAsFilePath("a", "b", url + "crap"));
        assertEquals("a" + s + "b.longsuffix",
                sanitizeAsFilePath("a", "b", url + "crap.longsuffix"));
        assertEquals("a" + s + "b.mp3", sanitizeAsFilePath("a", "b", url + "crap.mp3"));
        assertEquals("a" + s + "b", sanitizeAsFilePath("a", "b", url + "crap.mp3."));
        assertEquals("a" + s + "b.3", sanitizeAsFilePath("a", "b", url + "crap.mp.3"));
        assertEquals("a" + s + "b.avi", sanitizeAsFilePath("a?:>", "b!#,", url + "crap.avi"));
        assertEquals("a" + s + "b.avi", sanitizeAsFilePath("a?:>", "b!#,", url + "crap.avi!"));
        assertEquals("a" + s + "b.avi", sanitizeAsFilePath("a?:>", "b!#,", url + "crap.path.avi"));
        assertEquals("ah" + s + "bla.mp4", sanitizeAsFilePath("|[]a?:>h", ":>bl>a!#,",
                "http://www.hello.com/podcast/episode.mp4.mp4"));
        assertEquals("ah" + s + "bla.mp4", sanitizeAsFilePath("|[]a?:>h", ":>bl>a!#,",
                "http://www.hello.com/podcast/~r/crazy/episode.mp4.mp4"));

        assertEquals("This American Life" + s + "14 Accidental Documentaries.mp3",
                sanitizeAsFilePath("This American Life", "#14: Accidental Documentaries",
                        "http://feeds.thisamericanlife.org/~r/talpodcast/~5/WiJ-Ef0cNsM/14.mp3")
        );
        assertEquals(
                "Linux Outlaws" + s
                        + "Linux Outlaws 332 – International Football is a Bit Like War.mp3",
                sanitizeAsFilePath("Linux Outlaws",
                        "Linux Outlaws 332 – International Football is a Bit Like War",
                        "http://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/linuxoutlaws/linuxoutlaws332.mp3")
        );
    }
}
