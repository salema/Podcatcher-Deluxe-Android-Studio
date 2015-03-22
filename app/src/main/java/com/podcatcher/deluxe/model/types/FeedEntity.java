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

import com.podcatcher.deluxe.model.tags.RSS;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The abstract root type of the main podcatcher app types, including
 * {@link Podcast}, {@link Episode}, and {@link Suggestion}. Defines some
 * members needed in all of them.
 */
public abstract class FeedEntity {

    /**
     * The date format used by RSS feeds
     */
    private static final String DATE_FORMAT_TEMPLATE = "EEE, dd MMM yy HH:mm:ss zzz";
    /**
     * Our formatter used when reading the episode item's date string
     */
    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat(DATE_FORMAT_TEMPLATE, Locale.US);
    /**
     * The alternative date formats supported because they are used by some
     * feeds, these are all tried in the given order if the default fails
     */
    private static final String[] DATE_FORMAT_TEMPLATE_ALTERNATIVES = {
            "EEE, dd MMM yy", "yy-MM-dd", "EEE,dd MMM yy HH:mm:ss zzz"
    };

    /**
     * Name of the entity
     */
    protected String name;
    /**
     * Location of the entity's file
     */
    protected String url;
    /**
     * Entity's description
     */
    protected String description;

    /**
     * Whether the element contains explicit language or pics
     */
    protected boolean explicit = false;
    /**
     * The element's file size
     */
    protected long fileSize = -1;

    /**
     * @return The entity's title. This might by empty or <code>null</code>.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * @return The entity's online location.
     */
    @Nullable
    public String getUrl() {
        return url;
    }

    /**
     * @return The entity's description.
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * @return Whether the entity is considered explicit, i.e. contains
     * adult-only material.
     */
    public boolean isExplicit() {
        return explicit;
    }

    /**
     * @return The elements file size as downloaded or given by the feed in bytes.
     * Value of -1 if the size is not available.
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Set new file size for the feed entity.
     *
     * @param fileSize Size in bytes, values below or equal zero are ignored.
     */
    public void setFileSize(long fileSize) {
        if (fileSize > 0)
            this.fileSize = fileSize;
    }

    /**
     * Normalize the given URL string. See
     * http://en.wikipedia.org/wiki/URL_normalization for details.
     *
     * @param spec The URL string to normalize.
     * @return The same URL string with unchanged semantics, but normalized
     * syntax. When not a valid URL or <code>null</code>, the string
     * given is returned unaltered.
     */
    @Nullable
    protected String normalizeUrl(final @Nullable String spec) {
        try {
            // Trim white spaces, normalize path, throw exception if mal-formed
            final URL url = new URI(spec.trim()).normalize().toURL();

            // Make sure protocol and server are lower case
            final String scheme = url.getProtocol().toLowerCase(Locale.US);
            final String host = url.getHost().toLowerCase(Locale.US);

            // Normalize path to be at least "/"
            String path = url.getPath();
            if (path == null || path.isEmpty())
                path = "/";
            else if (path.length() > 1 && path.endsWith("/"))
                path = path.substring(0, path.length() - 1);

            // Look at ports and only keep non-defaults
            boolean needsPort = url.getPort() != -1;
            if ((scheme.equals("http") && url.getPort() == 80)
                    || (scheme.equals("https") && url.getPort() == 443))
                needsPort = false;

            // Reconstruct the string
            return scheme + "://" + host + (needsPort ? ":" + url.getPort() : "")
                    + path + (url.getQuery() == null ? "" : "?" + url.getQuery());
        } catch (MalformedURLException | NullPointerException | URISyntaxException
                | IllegalArgumentException e) {
            // We simply return the original string
            return spec;
        }
    }

    /**
     * Check whether the given string values indicated that the feed entity is
     * considered explicit.
     *
     * @param value The string value from the feed, <code>null</code> results in <code>false</code>.
     * @return The explicit flag.
     */
    protected boolean parseExplicit(@Nullable String value) {
        return value != null
                && value.trim().toLowerCase(Locale.US).equals(RSS.EXPLICIT_POSITIVE_VALUE);
    }

    /**
     * Parse a string into a date. Can be used for last feed updates or
     * publication dates. The method will try to read different formats.
     *
     * @param dateString The string from the RSS/XML feed to parse.
     * @return The date or <code>null</code> if the string could not be parsed.
     */
    @Nullable
    protected Date parseDate(@NonNull String dateString) {
        try {
            // SimpleDateFormat is not thread safe
            synchronized (DATE_FORMATTER) {
                return DATE_FORMATTER.parse(dateString);
            }
        } catch (ParseException e) {
            // The default format is not available, try all the other formats we
            // support...
            for (String format : DATE_FORMAT_TEMPLATE_ALTERNATIVES)
                try {
                    return new SimpleDateFormat(format, Locale.US).parse(dateString);
                } catch (ParseException e1) {
                    // Does not fit the format, pass and try next
                }
        }

        // None of the formats matched
        return null;
    }
}
