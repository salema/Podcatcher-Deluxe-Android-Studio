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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Genre (category) of the podcast.
 */
@SuppressWarnings("javadoc")
public enum Genre {
    ARTS("Arts"),
    BUSINESS("Business"),
    COMEDY("Comedy"),
    EDUCATION("Education"),
    GAMES_HOBBIES("Games & Hobbies"),
    GOVERNMENT_ORGANIZATIONS("Government & Organizations"),
    HEALTH("Health"),
    KIDS_FAMILY("Kids & Family"),
    MUSIC("Music"),
    NEWS_POLITICS("News & Politics"),
    RELIGION_SPIRITUALITY("Religion & Spirituality"),
    SCIENCE_MEDICINE("Science & Medicine"),
    SOCIETY_CULTURE("Society & Culture"),
    SPORTS_RECREATION("Sports & Recreation"),
    TECHNOLOGY("Technology"),
    TV_FILM("TV & Film");

    private final String label;

    Genre(String label) {
        this.label = label;
    }

    /**
     * Find the genre by a given label.
     *
     * @param label The label to search for.
     * @return The genre instance.
     * @throws IllegalArgumentException If the label is not recognized.
     */
    public static Genre forLabel(String label) {
        for (Genre genre : Genre.values())
            if (genre.label.toLowerCase(Locale.US).equals(label.toLowerCase(Locale.US).trim()))
                return genre;

        throw new IllegalArgumentException("Label \"" + label + "\" does not match any genre!");
    }

    /**
     * Converts a list of genres to a set of enum values.
     *
     * @param list      The plain JSON string to parse, e.g. "Music, Technology".
     * @param delimiter The delimiter used in the JSON string.
     * @return A set of parsed genre values. Might be empty, but never <code>null</code>.
     */
    @NonNull
    public static Set<Genre> valueOfJson(@Nullable String list, @Nullable String delimiter) {
        final HashSet<Genre> result = new HashSet<>();

        if (list != null && delimiter != null) {
            final String[] genres = list.split(delimiter);
            for (String genre : genres)
                try {
                    result.add(forLabel(genre));
                } catch (IllegalArgumentException iae) {
                    // Bad string, skip...
                }
        }

        return result;
    }
}
