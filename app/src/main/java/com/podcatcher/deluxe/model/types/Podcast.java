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

package com.podcatcher.deluxe.model.types;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Base64;

import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.tags.RSS;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * The podcast type. This represents the most important type in the podcatcher
 * application. To create a podcast, give its name and an online location to
 * load its RSS/XML file from. The online location is not verified or checked
 * inside the podcast type.
 * <p>
 * <b>Parsing:</b> Call {@link #parse(XmlPullParser)} with the parser set up to
 * the correct content in order to make the podcast object read and refresh its
 * members. Use {@link #getLastLoaded()} to find out whether and when this last
 * happened to a given podcast instance.
 * </p>
 * <p>
 * <b>Comparisons and Equals:</b> For the purpose of the podcatcher app, two
 * podcasts are equal iff they point at the same online feed resource. The
 * {@link #compareTo(Podcast)} method works on the podcast's name though and is
 * therefore <em>not</em> consistent with {@link #equals(Object)}.
 * </p>
 * <p>
 * <b>Logo:</b> Podcast often have logos. This podcast type allows for access to
 * the logo's online location (after {@link #parse(XmlPullParser)}, of course).
 * </p>
 */
public class Podcast extends FeedEntity implements Comparable<Podcast> {

    /**
     * Broadcast language
     */
    protected Language language;
    /**
     * Podcast genre
     */
    protected Genre genre;
    /**
     * Podcast media type
     */
    protected MediaType mediaType;

    /**
     * The podcast feed file encoding
     */
    protected String feedEncoding;
    /**
     * The podcast's image (logo) location
     */
    protected String logoUrl;

    /**
     * Username for http authorization
     */
    protected String username;
    /**
     * Password for http authorization
     */
    protected String password;

    /**
     * The point in time when the RSS file as last been set
     */
    protected Date lastLoaded;
    /**
     * The podcasts list of episodes
     */
    protected List<Episode> episodes = new ArrayList<>();

    /**
     * The count of failed load attempts
     */
    private int failedLoadAttempts = 0;

    /**
     * Create a new podcast by name and RSS file location. The name will not be
     * read from the file, but remains as given (unless you give
     * <code>null</code> as the name). All other data on the podcast will only
     * be available after {@link #parse(XmlPullParser)} was called.
     *
     * @param name The podcast's name, if you give <code>null</code> the name
     *             will be read from the RSS file on
     *             {@link #parse(XmlPullParser)}.
     * @param url  The location of the podcast's RSS file (not <code>null</code>).
     * @see #parse(XmlPullParser)
     */
    public Podcast(@Nullable String name, @NonNull String url) {
        this.name = name;
        this.url = normalizeUrl(url);
    }

    /**
     * @return The language as in {@link com.podcatcher.deluxe.model.types.Language}.
     */
    @Nullable
    public Language getLanguage() {
        return language;
    }

    /**
     * @return The genre as in {@link com.podcatcher.deluxe.model.types.Genre}.
     */
    @Nullable
    public Genre getGenre() {
        return genre;
    }

    /**
     * @return The mediaType as in {@link com.podcatcher.deluxe.model.types.MediaType}.
     * This is <em>not</em> the media file type (e.g. audio/mpeg), you get this from the episodes.
     */
    @Nullable
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * @return The user name for this podcast. Maybe <code>null</code> if
     * unknown or unneeded.
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * Set the user name for this podcast.
     *
     * @param username Name to use. Give <code>null</code> to reset.
     */
    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    /**
     * @return The password for this podcast. Maybe <code>null</code> if unknown
     * or unneeded.
     */
    @Nullable
    public String getPassword() {
        return password;
    }

    /**
     * Set the password for this podcast.
     *
     * @param password Password to use. Give <code>null</code> to reset.
     */
    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    /**
     * @return Authorization string to be used as a HTTP request header or
     * <code>null</code> if user name or password are not set.
     */
    @Nullable
    public String getAuthorization() {
        String result = null;

        if (username != null && password != null) {
            final String userpass = username + ":" + password;
            final byte[] authBytes = userpass.getBytes(Charset.forName("UTF-8"));

            result = "Basic " + Base64.encodeToString(authBytes, Base64.NO_WRAP);
        }

        return result;
    }

    /**
     * Find and return all episodes for this podcast. Will never return
     * <code>null</code> but an empty list when encountering problems. Set and
     * parse the RSS file before expecting any results.
     *
     * @return The list of episodes as listed in the feed.
     * @see #parse(XmlPullParser)
     */
    @NonNull
    public List<Episode> getEpisodes() {
        // Need to return a copy, so nobody can change this on us and changes
        // made in the model do not make problems in the UI
        return new ArrayList<>(episodes);
    }

    /**
     * Clean all explicit episodes from the podcast. Once called, the
     * {@link #getEpisodes()} method will only return episodes <em>not</em>
     * marked explicit until {@link #parse(XmlPullParser)} is called.
     *
     * @return The number of clean episodes left.
     */
    public int removeExplicitEpisodes() {
        Iterator<Episode> episodeIterator = episodes.iterator();

        while (episodeIterator.hasNext()) {
            final Episode episode = episodeIterator.next();

            if (episode.isExplicit())
                episodeIterator.remove();
        }

        return episodes.size();
    }

    /**
     * @return The number of episode for this podcast (always >= 0).
     * @see #parse(XmlPullParser)
     */
    public int getEpisodeCount() {
        return episodes.size();
    }

    /**
     * @return The feed remote file encoding or <code>null</code> if not yet parsed or
     * detected by the parser.
     * @see #parse(XmlPullParser)
     */
    @Nullable
    public String getFeedEncoding() {
        return feedEncoding;
    }

    /**
     * Find and return the podcast's image location (logo).
     *
     * @return URL pointing at the logo location (might be <code>null</code> if the podcast
     * does not provide a logo or it has not been parsed).
     * @see #parse(XmlPullParser)
     */
    @Nullable
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Set the logo to use for this podcast.
     *
     * @param logoUrl URL to show logo pic from, <code>null</code> is ignored.
     */
    public void setLogoUrl(String logoUrl) {
        if (logoUrl != null)
            this.logoUrl = logoUrl;
    }

    /**
     * @return Whether the podcast has a valid logo URL.
     */
    public boolean hasLogoUrl() {
        return logoUrl != null && logoUrl.startsWith("http");
    }

    /**
     * @return The point in time this podcast has last been loaded or
     * <code>null</code> iff it had not been loaded before.
     */
    @Nullable
    public Date getLastLoaded() {
        return lastLoaded == null ? null : new Date(lastLoaded.getTime());
    }

    /**
     * Reset the failed count to zero.
     */
    public void resetFailedLoadAttempts() {
        this.failedLoadAttempts = 0;
    }

    /**
     * Increment the load failed count by one.
     */
    public void incrementFailedLoadAttempts() {
        this.failedLoadAttempts++;
    }

    /**
     * @return The number of failed loads as recorded by calls to
     * {@link #incrementFailedLoadAttempts()}.
     */
    public int getFailedLoadAttemptCount() {
        return this.failedLoadAttempts;
    }

    @Override
    public String toString() {
        return name + " at " + url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof Podcast))
            return false;

        final Podcast another = (Podcast) o;

        // Podcasts are equal iff they point at the same online content
        return url != null && url.equals(another.url);
    }

    @Override
    public int hashCode() {
        return 42 + (url == null ? 0 : url.hashCode());
    }

    @Override
    public int compareTo(@NonNull Podcast another) {
        if (name != null && another.name != null)
            return name.compareToIgnoreCase(another.name);
        else if (name == null && another.name != null)
            return -1;
        else if (name != null) // Always true: && another.name == null)
            return 1;
        else
            return 0;
    }

    /**
     * Rewrite given relative URL to an absolute URL for this podcast. If the
     * given URL is already absolute (or empty or <code>null</code>) it is
     * returned unchanged.
     *
     * @param relativeUrl URL to rewrite.
     * @return An absolute URL based on the podcast URL. E.g. if "test/pic.jpg"
     * is provided, "http://www.example.com/feed/test/pic.jpg" might be
     * returned.
     */
    @Nullable
    public String toAbsoluteUrl(@Nullable String relativeUrl) {
        String result = relativeUrl;

        // Rewrite logo url to be absolute
        if (url != null && relativeUrl != null && !relativeUrl.isEmpty()
                && Uri.parse(relativeUrl).isRelative()) {
            final Uri podcastUrl = Uri.parse(url);
            final String prefix = podcastUrl.getScheme() + "://" + podcastUrl.getAuthority();

            if (relativeUrl.startsWith("/"))
                result = prefix + relativeUrl;
            else {
                final String path = podcastUrl.getPath();

                result = prefix + path.substring(0, path.length()
                        - podcastUrl.getLastPathSegment().length()) + relativeUrl;
            }
        }

        return result;
    }

    /**
     * Set the RSS file parser representing this podcast. This is were the
     * object gets its information from. Many of its methods will not return
     * valid results unless this method was called. Calling this method resets
     * all episode information that might have been read earlier, other meta
     * data is preserved and will only change if the feed has actually changed.
     * Episode information is preserved, however, if parsing actually fails. In
     * this case the episode list will not be altered.
     *
     * @param parser Parser used to read the RSS/XML file.
     * @throws IOException            If we encounter problems read the file.
     * @throws XmlPullParserException On parsing errors.
     */
    public void parse(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        final List<Episode> newEpisodes = new ArrayList<>();

        try {
            // Start parsing
            this.feedEncoding = parser.getInputEncoding();
            int eventType = parser.next();
            int episodeIndex = 0;

            // Read complete document
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // We only need start tags here
                if (eventType == XmlPullParser.START_TAG) {
                    final String tagName = parser.getName().toLowerCase(Locale.US);

                    switch (tagName) {
                        case RSS.TITLE:
                            if (name == null || name.trim().isEmpty())
                                name = Html.fromHtml(parser.nextText().trim()).toString();
                            break;
                        case RSS.EXPLICIT:
                            explicit = parseExplicit(parser.nextText());
                            break;
                        case RSS.IMAGE:
                            parseLogo(parser);
                            break;
                        case RSS.THUMBNAIL:
                            if (!hasLogoUrl())
                                logoUrl = parser.getAttributeValue("", RSS.URL);
                            break;
                        case RSS.ITEM:
                            parseAndAddEpisode(parser, newEpisodes, episodeIndex++);
                            break;
                        default:
                            parse(parser, tagName);
                    }
                }

                // Done, get next parsing event
                eventType = parser.next();
            }

            // Parsing completed without errors, mark as updated
            this.episodes = newEpisodes;
            this.lastLoaded = new Date();
        } finally {
            // Make sure name is not empty
            if (name == null || name.trim().isEmpty())
                name = url;
        }
    }

    /**
     * Called for tags not consumed by the podcast parsing. Use this in sub-classes,
     * if you need to get more data from the podcast feed. This will <em>not</em> be
     * called for tags the podcast consumes itself.
     *
     * @param parser  The current parser. Make sure to only consume the current tag!
     * @param tagName The current tag's name, all lower case.
     */
    protected void parse(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        // Do nothing, subclass might want to use this hook
        // to read other information they care about from the feed
    }

    protected void parseLogo(@NonNull XmlPullParser parser) throws IOException {
        try {
            // Check for href attribute (of <itunes:image> tag)
            final String href = parser.getAttributeValue("", RSS.HREF);

            if (href != null)
                logoUrl = toAbsoluteUrl(href);
            else if (logoUrl == null) {
                // URL tag used instead. We do not override any previous setting, because
                // the href is from the <itunes:image> tag which tends to have better pics.
                parser.require(XmlPullParser.START_TAG, "", RSS.IMAGE);

                // Look at all start tags of this image
                while (parser.nextTag() == XmlPullParser.START_TAG) {
                    // URL tag found
                    if (parser.getName().equalsIgnoreCase(RSS.URL))
                        logoUrl = toAbsoluteUrl(parser.nextText());
                        // Unneeded node, skip...
                    else
                        ParserUtils.skipSubTree(parser);
                }

                // Make sure we end at image tag
                parser.require(XmlPullParser.END_TAG, "", RSS.IMAGE);
            }
        } catch (XmlPullParserException e) {
            // The podcast logo information could not be read from the RSS file,
            // skip...
        }
    }

    protected void parseAndAddEpisode(@NonNull XmlPullParser parser,
                                      @NonNull List<Episode> list, int index) {
        // Create episode and parse the data
        final Episode newEpisode = new Episode(this, index);

        try {
            newEpisode.parse(parser);

            // Only add if there is a title and some actual content to play
            final String title = newEpisode.getName();
            if (title != null && !title.trim().isEmpty() &&
                    newEpisode.getMediaUrl() != null)
                list.add(newEpisode);
        } catch (XmlPullParserException | IOException e) {
            // pass, episode will not be added
        }
    }

    @Override
    @Nullable
    protected String normalizeUrl(@Nullable String spec) {
        // We put some extra bit in here to that only apply to podcast URLs and
        // then call the base class method.
        if (spec != null) {
            if (spec.toLowerCase(Locale.US).startsWith("feed://") ||
                    spec.toLowerCase(Locale.US).startsWith("itpc://") ||
                    spec.toLowerCase(Locale.US).startsWith("itms://"))
                spec = "http" + spec.substring(4);
            if (spec.toLowerCase(Locale.US).startsWith("fb:"))
                spec = "http://feeds.feedburner.com/" + spec.substring(3);
        }

        spec = super.normalizeUrl(spec);

        if (spec != null) {
            if (spec.startsWith("http://feeds2.feedburner.com"))
                spec = spec.replaceFirst("feeds2", "feeds");
            if (spec.startsWith("https://feeds2.feedburner.com"))
                spec = spec.replaceFirst("feeds2", "feeds");
            if (spec.contains("://feeds.feedburner.com") && spec.endsWith("?format=xml"))
                spec = spec.replace("?format=xml", "");
        }

        return spec;
    }
}
