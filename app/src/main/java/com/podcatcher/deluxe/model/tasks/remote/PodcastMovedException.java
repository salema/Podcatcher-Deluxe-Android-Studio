/**
 * Copyright 2012-2015 Kevin Hausmann
 * <p/>
 * This file is part of Podcatcher Deluxe.
 * <p/>
 * Podcatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p/>
 * Podcatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Podcatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */
package com.podcatcher.deluxe.model.tasks.remote;

/**
 * Podcast feed parsing exception thrown to indicate that the podcast
 * currently loading has moved to a new location and should be read from there.
 */
public class PodcastMovedException extends Exception {

    private final String newUrl;

    /**
     * Create the exception.
     *
     * @param url The podcast's new home.
     */
    public PodcastMovedException(String url) {
        this.newUrl = url;
    }

    /**
     * @return The address of the podcast's new home.
     */
    public String getNewUrl() {
        return newUrl;
    }
}
