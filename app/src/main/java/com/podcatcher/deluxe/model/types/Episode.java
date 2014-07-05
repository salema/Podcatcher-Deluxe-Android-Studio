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

package com.podcatcher.deluxe.model.types;

import android.text.Html;

import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.tags.RSS;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;

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
     * Create a new episode.
     *
     * @param podcast The podcast this episode belongs to. Cannot be
     *                <code>null</code>.
     * @param index   The index of the episode created in the podcast's feed (used
     *                for sorting if the publication date is not available).
     */
    public Episode(Podcast podcast, int index) {
        if (podcast == null)
            throw new NullPointerException("Episode can not have null as the podcast instance!");

        this.podcast = podcast;
        this.index = index;
    }

    /**
     * Create a new episode and set all fields manually.
     *
     * @param podcast     Podcast this episode belongs to. Cannot be
     *                    <code>null</code>.
     * @param name        Episode name.
     * @param mediaUrl    The remote URL of this episode.
     * @param pubDate     The publication date.
     * @param description The episode's description.
     */
    Episode(Podcast podcast, String name, String mediaUrl, Date pubDate, String description) {
        this(podcast, -1);

        this.name = name;
        this.mediaUrl = mediaUrl;
        this.description = description;
        // Publication date might not be present
        if (pubDate != null)
            this.pubDate = new Date(pubDate.getTime());
    }

    /**
     * @return The owning podcast. This will not be <code>null</code>.
     */
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
     * @return The media content online location.
     */
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     * @return The publication date for this episode.
     */
    public Date getPubDate() {
        if (pubDate == null)
            return null;
        else
            return new Date(pubDate.getTime());
    }

    /**
     * @return The episode's duration as given by the podcast feed converted
     * into a string 00:00:00. This might not be available and therefore
     * <code>null</code>.
     */
    public String getDurationString() {
        return duration > 0 ? ParserUtils.formatTime(duration) : null;
    }

    /**
     * @return The episode's duration in seconds as given by the podcast feed or
     * -1 if not available.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @return The long content description for this episode from the
     * content:encoded tag (if any). Might be <code>null</code>.
     */
    public String getLongDescription() {
        return content;
    }

    @Override
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

        return mediaUrl == null ? false : mediaUrl.equals(another.mediaUrl);
    }

    @Override
    public int hashCode() {
        return 42 + (mediaUrl == null ? 0 : mediaUrl.hashCode());
    }

    @Override
    public int compareTo(Episode another) {
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
        else if (this.pubDate != null && another.pubDate == null)
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
     * @param parser Podcast file parser, set to the start tag of the item to
     *               read.
     * @throws XmlPullParserException On parsing problems.
     * @throws IOException            On I/O problems.
     */
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Make sure we start at item tag
        parser.require(XmlPullParser.START_TAG, "", RSS.ITEM);

        // Look at all start tags of this item
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            final String tagName = parser.getName();

            // Episode title
            if (tagName.equalsIgnoreCase(RSS.TITLE))
                name = Html.fromHtml(parser.nextText().trim()).toString();
                // Episode online location
            else if (tagName.equalsIgnoreCase(RSS.LINK))
                url = parser.nextText();
                // Explicit info found
            else if (tagName.equalsIgnoreCase(RSS.EXPLICIT))
                explicit = parseExplicit(parser.nextText());
                // Episode media URL
            else if (tagName.equalsIgnoreCase(RSS.ENCLOSURE)) {
                // Only set the media URL if it is actually there, this will
                // prevent overriding it when there are multiple enclosures
                final String urlAttribute = parser.getAttributeValue("", RSS.URL);
                if (urlAttribute != null)
                    mediaUrl = normalizeUrl(urlAttribute);

                parser.nextText();
            }
            // Episode publication date (2 options)
            else if (tagName.equalsIgnoreCase(RSS.DATE) && pubDate == null)
                pubDate = parseDate(parser.nextText());
            else if (tagName.equalsIgnoreCase(RSS.PUBDATE))
                pubDate = parseDate(parser.nextText());
                // Episode duration
            else if (tagName.equalsIgnoreCase(RSS.DURATION))
                duration = parseDuration(parser.nextText());
                // Episode description
            else if (tagName.equalsIgnoreCase(RSS.DESCRIPTION))
                description = parser.nextText();
            else if (isContentEncodedTag(parser))
                content = parser.nextText();
                // Unneeded node, skip...
            else
                ParserUtils.skipSubTree(parser);
        }

        // Make sure we end at item tag
        parser.require(XmlPullParser.END_TAG, "", RSS.ITEM);
    }

    protected int parseDuration(String durationString) {
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
            } catch (NumberFormatException ex) {
                // Pass, duration not available
            } catch (NullPointerException nex) {
                // Pass, duration string is null
            }
        }

        // Never return zero as a duration since that does not make sense.
        return result == 0 ? -1 : result;
    }

    protected boolean isContentEncodedTag(XmlPullParser parser) {
        return RSS.CONTENT_ENCODED.equals(parser.getName()) &&
                RSS.CONTENT_NAMESPACE.equals(parser.getNamespace(parser.getPrefix()));
    }
}
