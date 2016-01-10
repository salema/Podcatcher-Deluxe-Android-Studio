/**
 * Copyright 2012-2016 Kevin Hausmann
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
 * Podcast media type.
 */
@SuppressWarnings("javadoc")
public enum MediaType {
    AUDIO, VIDEO;

    /**
     * Get media type for a given mime type.
     *
     * @param mimeType The mime type string, e.g. "audio/mpeg3".
     * @return The corresponding media type.
     * @throws IllegalArgumentException If the mime type is not recognized.
     */
    public static MediaType forMimeType(String mimeType) {
        return MediaType.valueOf(mimeType.split("/")[0].toUpperCase(Locale.US).trim());
    }

    /**
     * Converts a list of media types to a set of enum values.
     *
     * @param list      The plain JSON string to parse, e.g. "Audio, Video".
     * @param delimiter The delimiter used in the JSON string.
     * @return A set of parsed type values. Might be empty, but never <code>null</code>.
     */
    @NonNull
    public static Set<MediaType> valueOfJson(@Nullable String list, @Nullable String delimiter) {
        final HashSet<MediaType> result = new HashSet<>();

        if (list != null && delimiter != null) {
            final String[] types = list.split(delimiter);
            for (String type : types)
                try {
                    result.add(MediaType.valueOf(type.toUpperCase(Locale.US).trim()));
                } catch (IllegalArgumentException iae) {
                    // Bad string, skip...
                }
        }

        return result;
    }
}
