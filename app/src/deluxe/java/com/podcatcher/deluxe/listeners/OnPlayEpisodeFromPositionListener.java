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


import com.podcatcher.deluxe.model.types.Episode;

/**
 * Interface definition for a listener to be called when the user requests
 * the playback of a certain episode to start from given position.
 */
public interface OnPlayEpisodeFromPositionListener {

    /**
     * Called on the listener if it needs to start playing the given episode from given position.
     *
     * @param episode Episode to play.
     * @param millis  Position in media stream to start playback at (in milli-seconds).
     */
    void onPlayFromPosition(Episode episode, int millis);
}
