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

package com.podcatcher.deluxe.listeners;

import android.net.Uri;

import com.podcatcher.deluxe.model.types.Podcast;

import java.util.List;

/**
 * Interface definition for a callback to be invoked when a podcast list is
 * written to disk.
 */
public interface OnStorePodcastListListener {

    /**
     * Called on successful completion.
     *
     * @param podcastList Podcast list stored.
     * @param outputFile  The destination the list was written to.
     */
    public void onPodcastListStored(List<Podcast> podcastList, Uri outputFile);

    /**
     * Called on failure.
     *
     * @param podcastList Podcast list attempted to store.
     * @param outputFile  The destination the list was not written to.
     * @param exception   The reason for the failure.
     */
    public void onPodcastListStoreFailed(List<Podcast> podcastList, Uri outputFile,
                                         Exception exception);
}
