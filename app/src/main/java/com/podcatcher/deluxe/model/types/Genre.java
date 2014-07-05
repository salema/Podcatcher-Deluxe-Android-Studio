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

import java.util.Locale;

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
}
