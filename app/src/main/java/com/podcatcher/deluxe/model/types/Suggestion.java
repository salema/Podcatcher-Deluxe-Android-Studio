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

import java.util.Set;

/**
 * The podcast suggestion type. Extends {@link Podcast} by a few fields and
 * methods specific to suggestions.
 */
public class Suggestion extends Podcast {

    /**
     * Whether the podcast is featured.
     * The UI might later decide to highlight featured suggestions.
     */
    protected boolean featured = false;

    /**
     * Whether the podcast suggestion is new.
     * The UI might later decide to highlight new suggestions.
     */
    protected boolean recent = false;

    /**
     * Create new suggestion. See {@link Podcast} for details.
     *
     * @param name The name to show.
     * @param url  The URL to load feed from.
     */
    public Suggestion(@NonNull String name, @NonNull String url) {
        super(name, url);
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    /**
     * @param languages The podcast content language(s) to set.
     */
    public void setLanguages(@NonNull Set<Language> languages) {
        this.languages = languages;
    }

    /**
     * @param genres The podcast categories to set.
     */
    public void setGenres(@NonNull Set<Genre> genres) {
        this.genres = genres;
    }

    /**
     * @param mediaTypes The media type(s) to set.
     */
    public void setMediaTypes(@NonNull Set<MediaType> mediaTypes) {
        this.mediaTypes = mediaTypes;
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
     * @return Whether this suggestion has been added lately.
     */
    public boolean isNew() {
        return recent;
    }

    /**
     * @param recent What to set the flag to.
     */
    public void setNew(boolean recent) {
        this.recent = recent;
    }

    /**
     * Mark a suggestion as containing adult-only material.
     *
     * @param explicit The flag to set.
     */
    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public boolean equals(Object o) {
        // We do not need to do anything special here, suggestions are equal if
        // the podcasts they represent are equal
        return super.equals(o);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public int hashCode() {
        // dito
        return super.hashCode();
    }
}
