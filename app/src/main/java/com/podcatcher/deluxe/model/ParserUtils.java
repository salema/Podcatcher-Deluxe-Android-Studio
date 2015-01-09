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

package com.podcatcher.deluxe.model;

import android.annotation.SuppressLint;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Utility class to support podcast XML/RSS parsing.
 */
public class ParserUtils {

    /**
     * A short time span format (no hours)
     */
    private static final String SHORT_DURATION = "%2$d:%3$02d";
    /**
     * A long time span format (with hours)
     */
    private static final String LONG_DURATION = "%d:%02d:%02d";

    /**
     * Format an amount of time.
     *
     * @param time Non-negative amount of seconds to format.
     * @return The time span as hh:mm:ss with appropriate omissions.
     */
    @SuppressLint("DefaultLocale")
    public static String formatTime(int time) {
        final int hours = time / 3600;

        return String.format(hours > 0 ? LONG_DURATION : SHORT_DURATION,
                hours, (time / 60) - 60 * hours, time % 60);
    }

    /**
     * Format an information given in bytes to the more familiar mega-bytes.
     *
     * @param bytes The number of bytes to calculate with
     * @return A string like "XMB" where X is the numeric representation of the size.
     */
    public static String formatFileSize(long bytes) {
        final int megaBytes = (int) bytes / (1024 * 1024);
        return megaBytes > 0 ? megaBytes + "MB" : bytes / 1024 + "KB";
    }

    /**
     * Skip the entire sub tree the given parser is currently pointing at.
     *
     * @param parser Parser to advance.
     * @throws XmlPullParserException On parsing problems.
     * @throws IOException            On I/O trouble.
     */
    public static void skipSubTree(XmlPullParser parser) throws XmlPullParserException, IOException {
        // We need to see a start tag next. The tag and any sub-tree it might
        // have will be skipped.
        parser.require(XmlPullParser.START_TAG, null, null);

        int level = 1;
        // Continue parsing and increase/decrease the level
        while (level > 0) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.END_TAG) {
                --level;
            } else if (eventType == XmlPullParser.START_TAG) {
                ++level;
            }
        }

        // We are back to the original level, behind the start tag given and any
        // sub-tree that might have been there. Return.
    }
}
