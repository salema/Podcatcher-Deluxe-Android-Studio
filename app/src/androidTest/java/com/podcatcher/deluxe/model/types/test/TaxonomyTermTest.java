/**
 * Copyright 2012-2015 Kevin Hausmann
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

package com.podcatcher.deluxe.model.types.test;

import com.podcatcher.deluxe.model.types.Genre;
import com.podcatcher.deluxe.model.types.Language;
import com.podcatcher.deluxe.model.types.MediaType;

import android.test.InstrumentationTestCase;

@SuppressWarnings("javadoc")
public class TaxonomyTermTest extends InstrumentationTestCase {

    public final void testGenreForLabel() {
        assertEquals(Genre.MUSIC, Genre.forLabel("Music"));
        assertEquals(Genre.MUSIC, Genre.forLabel(" MusiC "));
        assertEquals(Genre.SPORTS_RECREATION, Genre.forLabel("Sports & Recreation"));
    }

    public final void testGenreValueOfJson() {
        assertTrue(Genre.valueOfJson(null, null).isEmpty());
        assertTrue(Genre.valueOfJson("Music", null).isEmpty());
        assertTrue(Genre.valueOfJson(null, "|").isEmpty());
        assertTrue(Genre.valueOfJson("Test, Wrong Text ", ", ").isEmpty());
        assertTrue(Genre.valueOfJson(" mUSIC ", " | ").size() == 1);
        assertTrue(Genre.valueOfJson("Music, Sports & Recreation", ", ").size() == 2);

        assertTrue(Genre.valueOfJson("Comedy-Arts", "-").size() == 2);
        assertTrue(Genre.valueOfJson("Comedy-Arts", "-").contains(Genre.ARTS));
        assertTrue(Genre.valueOfJson("Comedy-Arts", "-").contains(Genre.COMEDY));
    }

    public final void testForMimeType() {
        assertEquals(MediaType.AUDIO, MediaType.forMimeType("audio/mpeg"));
        assertEquals(MediaType.AUDIO, MediaType.forMimeType("audio"));
        assertEquals(MediaType.VIDEO, MediaType.forMimeType("video/mp4"));
    }

    public final void testMediaTypeValueOfJson() {
        assertTrue(MediaType.valueOfJson(null, null).isEmpty());
        assertTrue(MediaType.valueOfJson("Audio", null).isEmpty());
        assertTrue(MediaType.valueOfJson(null, "|").isEmpty());
        assertTrue(MediaType.valueOfJson("Test, Wrong Text ", ", ").isEmpty());
        assertTrue(MediaType.valueOfJson(" video ", " | ").size() == 1);
        assertTrue(MediaType.valueOfJson("Audio, VIDEO", ", ").size() == 2);
    }

    public final void testForTwoLetterCode() {
        assertEquals(Language.ENGLISH, Language.forTwoLetterIsoCode("en"));
        assertEquals(Language.GERMAN, Language.forTwoLetterIsoCode("de"));
    }

    public final void testLanguageValueOfJson() {
        assertTrue(Language.valueOfJson(null, null).isEmpty());
        assertTrue(Language.valueOfJson("English", null).isEmpty());
        assertTrue(Language.valueOfJson(null, "|").isEmpty());
        assertTrue(Language.valueOfJson("Test, Wrong Text ", ", ").isEmpty());
        assertTrue(Language.valueOfJson(" french ", " | ").size() == 1);
        assertTrue(Language.valueOfJson("english, french", ", ").size() == 2);
    }
}
