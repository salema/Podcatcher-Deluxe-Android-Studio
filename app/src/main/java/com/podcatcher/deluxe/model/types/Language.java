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
 * Language a podcast is published in.
 */
@SuppressWarnings("javadoc")
public enum Language {
    ENGLISH("en"), FRENCH("fr"), GERMAN("de"), SPANISH("es"), ITALIAN("it"), CZECH("cs"),
    DUTCH("nl"), PORTUGUESE("pt"), RUSSIAN("ru"), SWEDISH("sv"), TURKISH("tr"), UKRAINIAN("uk");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    /**
     * Find the language by a ISO two letter country code.
     *
     * @param code The code to search for.
     * @return The language instance.
     * @throws IllegalArgumentException If the code is not recognized.
     */
    public static Language forTwoLetterIsoCode(String code) {
        for (Language language : Language.values())
            if (language.code.toLowerCase(Locale.US).equals(code.toLowerCase(Locale.US).trim()))
                return language;

        throw new IllegalArgumentException("Code \"" + code + "\" does not match any language!");
    }

    /**
     * Converts a list of languages to a set of enum values.
     *
     * @param list      The plain JSON string to parse, e.g. "English, German".
     * @param delimiter The delimiter used in the JSON string.
     * @return A set of parsed language values. Might be empty, but never <code>null</code>.
     */
    @NonNull
    public static Set<Language> valueOfJson(@Nullable String list, @Nullable String delimiter) {
        final HashSet<Language> result = new HashSet<>();

        if (list != null && delimiter != null) {
            final String[] languages = list.split(delimiter);
            for (String language : languages)
                try {
                    result.add(Language.valueOf(language.toUpperCase(Locale.US).trim()));
                } catch (IllegalArgumentException iae) {
                    // Bad string, skip...
                }
        }

        return result;
    }
}
