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

package com.podcatcher.deluxe.listeners;

import com.podcatcher.deluxe.model.types.Podcast;

import android.net.Uri;

import java.util.List;

/**
 * Interface definition for a callback to be invoked when a podcast list is
 * loaded.
 */
public interface OnLoadPodcastListListener {

    /**
     * Called on completion.
     *
     * @param podcastList Podcast list loaded.
     * @param inputFile   The file location loaded from.
     */
    void onPodcastListLoaded(List<Podcast> podcastList, Uri inputFile);

    /**
     * Called on failure.
     *
     * @param inputFile The file that failed to load as a podcast list.
     * @param error     The exception that occurred when trying to load the list
     */
    void onPodcastListLoadFailed(Uri inputFile, Exception error);
}
