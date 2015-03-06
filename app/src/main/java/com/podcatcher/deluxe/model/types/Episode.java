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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;

import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.tags.RSS;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

/**
 * The episode type. Each episode represents an item from a podcast's RSS/XML
 * feed. Episodes are created when the podcast is loaded (parsed), you should
 * have no need to create instances yourself.
 */
public class Episode extends FeedEntity implements Comparable<Episode> {

    /**
     * The podcast this episode is part of
     */
    protected final Podcast podcast;
    /**
     * The index (starting with zero at the top of the feed) this episode is in
     * its podcast. -1 means that we do not have this information.
     */
    protected final int index;

    /**
     * The episode's long content description
     */
    protected String content;
    /**
     * The episode's release date
     */
    protected Date pubDate;
    /**
     * The episode's duration
     */
    protected int duration = -1;
    /**
     * The episode's media file location
     */
    protected String mediaUrl;
    /**
     * The episode's media file type
     */
    protected String mediaType;

    /**
     * Create a new episode.
     *
     * @param podcast The podcast this episode belongs to. Cannot be <code>null</code>.
     * @param index   The index of the episode created in the podcast's feed (used
     *                for sorting if the publication date is not available). Giving any value
     *                below zero will have this ignored.
     */
    public Episode(@NonNull Podcast podcast, int index) {
        if (podcast == null)
            throw new NullPointerException("Episode can not have null as the podcast instance!");

        this.podcast = podcast;
        this.index = index < 0 ? -1 : index;
    }

    /**
     * Create a new episode and set all fields manually.
     *
     * @param podcast     Podcast this episode belongs to. Cannot be
     *                    <code>null</code>.
     * @param name        Episode name.
     * @param mediaUrl    The remote URL of this episode.
     * @param pubDate     The publication date.
     * @param mediaType   The episode's media type.
     * @param description The episode's description.
     */
    Episode(@NonNull Podcast podcast, String name, String mediaUrl, Date pubDate,
            String mediaType, String description) {
        this(podcast, -1);

        this.name = name;
        this.mediaUrl = mediaUrl;
        this.description = description;
        this.mediaType = mediaType;
        // Publication date might not be present
        if (pubDate != null)
            this.pubDate = new Date(pubDate.getTime());
    }

    /**
     * @return The owning podcast. This will not be <code>null</code>.
     */
    @NonNull
    public Podcast getPodcast() {
        return podcast;
    }

    /**
     * @return The index for this episode object in the podcast's feed. -1 means
     * that this information is not available.
     */
    public int getPositionInPodcast() {
        return index;
    }

    /**
     * @return The publication date for this episode or <code>null</code> if not present.
     */
    @Nullable
    public Date getPubDate() {
        return pubDate == null ? null : new Date(pubDate.getTime());
    }

    /**
     * @return The media content online location. This should not be <code>null</code>
     * after parsing but could be if the episode is created independently.
     */
    @Nullable
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     * @return The media type as given by the feed (e.g. "audio/mpeg")
     * or <code>null</code> if not available.
     */
    @Nullable
    public String getMediaType() {
        return mediaType;
    }

    /**
     * @return The episode's duration in seconds as given by the podcast feed or
     * -1 if not available.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Set the episode duration, potentially overriding the information
     * read from the podcast feed.
     *
     * @param duration The duration in seconds, values below or equal zero will be ignored.
     */
    public void setDuration(int duration) {
        if (duration > 0)
            this.duration = duration;
    }

    /**
     * @return The long content description for this episode from the
     * content:encoded tag (if any). Might be <code>null</code>.
     */
    @Nullable
    public String getLongDescription() {
        return content;
    }

    /**
     * @return A link to the episode's web page. Might be <code>null</code> to
     * indicate that none is given by the podcast feed.
     */
    @Nullable
    public String getWebsiteUrl() {
        return url != null && url.startsWith("http") && !url.equals(mediaUrl) ? url : null;
    }

    @Override
    @Nullable
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof Episode))
            return false;

        Episode another = (Episode) o;

        return mediaUrl != null && mediaUrl.equals(another.mediaUrl);
    }

    @Override
    public int hashCode() {
        return 42 + (mediaUrl == null ? 0 : mediaUrl.hashCode());
    }

    @Override
    public int compareTo(@NonNull Episode another) {
        // We need to be "consistent with equals": only return 0 (zero) for
        // equal episodes. Failing to do so will cause episodes with equal
        // pubDates to mysteriously disappear when put in a SortedSet. At the
        // same time, we do accept equal episodes that have different dates,
        // because it does not hurt much.
        int result = 0;

        // We mainly compare by the publication date of the episodes. If these
        // are not available or are equal, we check for their position in the
        // podcast. As a last resort we simply return something <> 0.
        if (this.pubDate != null && another.pubDate != null)
            result = another.pubDate.compareTo(pubDate);
        else if (this.pubDate == null && another.pubDate != null)
            result = -1;
        else if (this.pubDate != null) // Always true: && another.pubDate == null)
            result = 1;

        // This should never be zero unless the episodes are equal, since a
        // podcast might publish two episodes at the same pubDate. If it is
        // (and the episodes are not equal) we use the original order from
        // the feed instead. If all that is not available we simply return
        // a consistent, non-zero integer since failing the do so would remove
        // the episode from sets.
        if (result == 0 && !this.equals(another)) {
            // The pubDates are equal, but episode are not, try index
            if (index >= 0 && another.index >= 0 && index != another.index)
                result = index - another.index;
                // As a last resort return a consistent, non-zero int
            else {
                final int lastResort = this.hashCode() - another.hashCode();
                result = lastResort == 0 ? -1 : lastResort;
            }
        }

        return result;
    }

    /**
     * Read data from an item node in the RSS/XML podcast file and use it to set
     * this episode's fields.
     *
     * @param parser Podcast feed file parser, set to the start tag of the item to read.
     * @throws XmlPullParserException On parsing problems.
     * @throws IOException            On I/O problems.
     */
    void parse(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        // Make sure we start at item tag
        parser.require(XmlPullParser.START_TAG, "", RSS.ITEM);

        // Look at all start tags of this item
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            final String tagName = parser.getName().toLowerCase(Locale.US);

            if (tagName.equals(RSS.TITLE))
                name = Html.fromHtml(parser.nextText().trim()).toString();
            else if (tagName.equals(RSS.LINK))
                url = parser.nextText();
            else if (tagName.equals(RSS.EXPLICIT))
                explicit = parseExplicit(parser.nextText());
            else if (tagName.equals(RSS.DATE) && pubDate == null)
                pubDate = parseDate(parser.nextText());
            else if (tagName.equals(RSS.PUBDATE))
                pubDate = parseDate(parser.nextText());
            else if (tagName.equals(RSS.DURATION))
                duration = parseDuration(parser.nextText());
            else if (tagName.equals(RSS.DESCRIPTION))
                description = parser.nextText();
            else if (isContentEncodedTag(parser))
                content = parser.nextText();
            else if (tagName.equals(RSS.ENCLOSURE))
                parseEnclosure(parser);
            else
                ParserUtils.skipSubTree(parser);
        }

        // Make sure we end at item tag
        parser.require(XmlPullParser.END_TAG, "", RSS.ITEM);
    }

    protected void parseEnclosure(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        final String urlAttribute = parser.getAttributeValue("", RSS.URL);
        final String typeAttribute = parser.getAttributeValue("", RSS.MEDIA_TYPE);
        final String lengthAttribute = parser.getAttributeValue("", RSS.MEDIA_LENGTH);

        // Only set the media URL if it is actually there, this will
        // prevent overriding it when there are multiple enclosures
        if (urlAttribute != null) {
            mediaUrl = normalizeUrl(urlAttribute);
            mediaType = typeAttribute != null && typeAttribute.trim().length() > 0 ?
                    typeAttribute.trim() : null;
            try {
                fileSize = Integer.valueOf(lengthAttribute);
            } catch (NumberFormatException nfe) {
                // pass, length not available
                fileSize = -1;
            }
        }

        parser.nextText();
    }

    protected int parseDuration(@NonNull String durationString) {
        int result = -1;

        try {
            // Duration simply given as number of seconds
            result = Integer.parseInt(durationString);
        } catch (NumberFormatException e) {
            // The duration is given as something like "1:12:34" instead
            try {
                final String[] split = durationString.split(":");

                // e.g. 12:34
                if (split.length == 2)
                    result = Integer.parseInt(split[1]) + Integer.parseInt(split[0]) * 60;
                    // e.g. 01:12:34
                else if (split.length == 3)
                    result = Integer.parseInt(split[2]) + Integer.parseInt(split[1]) * 60
                            + Integer.parseInt(split[0]) * 3600;
            } catch (NumberFormatException | NullPointerException ex) {
                // Pass, duration not available or null
            }
        }

        // Never return zero as a duration since that does not make sense.
        return result == 0 ? -1 : result;
    }

    protected boolean isContentEncodedTag(@NonNull XmlPullParser parser) {
        return RSS.CONTENT_ENCODED.equals(parser.getName()) &&
                RSS.CONTENT_NAMESPACE.equals(parser.getNamespace(parser.getPrefix()));
    }

    @Override
    @Nullable
    protected String normalizeUrl(@Nullable String spec) {
        spec = super.normalizeUrl(spec);

        // Try to rescue bad media file URLs
        if (spec != null && !spec.startsWith("http"))
            try {
                final URL url = new URL(spec);
                final String scheme = url.getProtocol();

                if (!scheme.equals("http") && !scheme.equals("https"))
                    spec = new URL("http", url.getHost(), -1, url.getFile()).toExternalForm();
            } catch (MalformedURLException mue) {
                // we simply try returning the original string starting with http://
                spec = "http://" + spec;
            }


        return spec;
    }
}
