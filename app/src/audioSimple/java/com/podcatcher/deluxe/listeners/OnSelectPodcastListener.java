/** Copyright 2012-2014 Kevin Hausmann
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

/**
 * Interface definition for a callback to be invoked when a podcast is selected.
 */
public interface OnSelectPodcastListener {

    /**
     * Called on listener to reflect that a podcast has been selected.
     *
     * @param selectedPodcast Podcast selected by the user (not
     *                        <code>null</code>).
     */
    public void onPodcastSelected(Podcast selectedPodcast);

    /**
     * Called on listener to reflect that all podcasts are selected.
     */
    public void onAllPodcastsSelected();

    /**
     * Called on listener to reflect that downloads are selected.
     */
    public void onDownloadsSelected();

    /**
     * Called on listener to reflect that no podcast is selected anymore.
     */
    public void onNoPodcastSelected();
}
