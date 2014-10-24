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
 * Defines some constants used in OPML.
 *
 * @see <a href="http://dev.opml.org/spec2.html">OPML specification</a>
 */
@SuppressWarnings("javadoc")
public abstract class OPML {
    public static final String OUTLINE = "outline";
    public static final String TEXT = "text";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String RSS_TYPE = "rss";
    public static final String XMLURL = "xmlUrl";

    // Podcatcher Deluxe name space
    public static final String PCD_NAMESPACE = "http://www.podcatcher-deluxe.com/opml-ext";
    public static final String PCD_NS_PREFIX = "pcd:";
    // Extra outline attributes defined by Podcatcher Deluxe
    public static final String PCD_LOGO = "logo";
    public static final String PCD_USER = "user";
    public static final String PCD_PASS = "pass";
}
