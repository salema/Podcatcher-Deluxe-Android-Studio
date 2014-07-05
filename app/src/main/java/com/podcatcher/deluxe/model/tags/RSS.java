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

package com.podcatcher.deluxe.model.tags;

/**
 * Defines some constants used in RSS.
 *
 * @see <a href="http://cyber.law.harvard.edu/rss/rss.html">RSS
 * specification</a>
 */
@SuppressWarnings("javadoc")
public class RSS {
    public static final String ITEM = "item";
    public static final String IMAGE = "image";
    public static final String TITLE = "title";
    public static final String EXPLICIT = "explicit";
    public static final String ENCLOSURE = "enclosure";
    public static final String URL = "url";
    public static final String TYPE = "type";
    public static final String HREF = "href";
    public static final String LINK = "link";
    public static final String DATE = "date";
    public static final String PUBDATE = "pubDate";
    public static final String DURATION = "duration";
    public static final String THUMBNAIL = "thumbnail";
    public static final String DESCRIPTION = "description";
    public static final String CONTENT_ENCODED = "encoded";

    public static final String CONTENT_NAMESPACE = "http://purl.org/rss/1.0/modules/content/";
    public static final String EXPLICIT_POSITIVE_VALUE = "yes";
}
