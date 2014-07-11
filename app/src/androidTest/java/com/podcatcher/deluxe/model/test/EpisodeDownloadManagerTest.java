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

        assertEquals(
                "For Army Reserve Soldier Michael Cross the world as he knew it ended in an instant" + s
                        + "Live from Hollywood CA by way of the Broken Skull Ranch Pro Wrestling Hall of Famer.mp3",
                sanitizeAsFilePath(
                        "For Army Reserve Soldier Michael Cross, the world as he knew it ended in an instant. One minute, he&#39;s in college, and in the next, rioters are roaming the highway around him, breaking into cars, and literally tearing people apart. This is the day the dead walk. This is the world of We&#39;re Alive. We’re Alive is an ongoing series, packing performances and sound effects that rival movies and prove that modern audio drama is undead and well. Join our survivors as they band together, struggle to fortify a safe haven known as the Tower, and discovers that zombies are far from the worst thing in a post-apocalyptic Los Angeles where the rules of human decency no longer apply. We premiere 3 New episodes a month, with a week off between chapters. Little food. Little water. Little hope. Who is lucky enough to say &quot;We&#39;re Alive?&quot;",
                        "Live from Hollywood, CA by way of the Broken Skull Ranch, Pro Wrestling Hall of Famer, Action Movie/TV star, Steve Austin lets loose on these no-holds barred, explicit versions of the program. Steve gets down and dirty with Hollywood celebrities, past wrestling buddies, present pros, MMA fighters, athletes, movie stuntmen and from time to time, you the working man (or woman). Got questions? [questions@steveaustinshow.com](mailto:questions@steveaustinshow.com) [Hear the Tuesday Edition of The Steve Austin Show (Family, Friendly Edition), click here!](http://podcastone.com/Steve-Austin-Show-Clean) [ ![Sponsor](http://www.launchpaddigitalmedia.com/images/general /Button_Support_The_Sponsors-smaller.jpg)](http://www.podcastone.com/g/Steve- Austin-Show-Sponsors/453.html)",
                        url + "x.mp3")
        );
    }
}
