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

package com.podcatcher.deluxe.view;

import android.text.format.DateUtils;

import com.podcatcher.deluxe.model.types.Episode;

/**
 * Some utility functions used in the view.
 */
public class Utils {

    /**
     * Create a nice user-friendly publication date string by means of
     * {@link DateUtils#getRelativeTimeSpanString(long)}.
     *
     * @param episode The Episode to create date string for. Cannot be
     *                <code>null</code> and needs valid publication date.
     * @return A relative date string describing the relation between 'now' and
     * the episode's release time or an empty string ("") if the
     * publication date cannot be determined for the episode.
     */
    public static String getRelativePubDate(Episode episode) {
        if (episode != null && episode.getPubDate() != null) {
            final long pubTime = episode.getPubDate().getTime();

            // Get a nice time span string for the age of the episode
            String dateString = DateUtils.getRelativeTimeSpanString(pubTime,
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS).toString();

            // Make sure date string starts with upper case
            if (dateString.length() > 1 && Character.isLetter(dateString.charAt(0)))
                dateString = Character.toUpperCase(dateString.charAt(0)) + dateString.substring(1);

            return dateString;
        } else
            return "";
    }
}
