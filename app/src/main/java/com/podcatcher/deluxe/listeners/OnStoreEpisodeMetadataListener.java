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

/**
 * Interface definition for a callback to be invoked when the episode meta data
 * is written to disk.
 */
public interface OnStoreEpisodeMetadataListener {

    /**
     * Called on successful completion.
     */
    void onEpisodeMetadataStored();

    /**
     * Called on failure.
     *
     * @param exception The reason for the failure.
     */
    void onEpisodeMetadataStoreFailed(Exception exception);
}
