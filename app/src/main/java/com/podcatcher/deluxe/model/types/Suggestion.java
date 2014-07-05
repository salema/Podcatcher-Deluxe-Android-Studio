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

/**
 * The podcast suggestion type. Extends {@link Podcast} by a few fields and
 * methods specific to suggestions.
 */
public class Suggestion extends Podcast {

    /**
     * Whether the podcast is featured
     */
    protected boolean featured = false;

    /**
     * Create new suggestion. See {@link Podcast} for details.
     *
     * @param name The name to show.
     * @param url  The URL to load feed from.
     */
    public Suggestion(String name, String url) {
        super(name, url);
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(Language language) {
        this.language = language;
    }

    /**
     * @param genre The genre to set.
     */
    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    /**
     * @param mediaType The mediaType to set.
     */
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * @return Whether this suggestion is featured.
     */
    public boolean isFeatured() {
        return featured;
    }

    /**
     * @param featured What to set the flag to.
     */
    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    /**
     * Mark a suggestion as containing adult-only material.
     *
     * @param explicit The flag to set.
     */
    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    @Override
    public boolean equals(Object o) {
        // We do not need to do anything special here, suggestions are equal if
        // the podcasts they represent are equal
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        // dito
        return super.hashCode();
    }
}
