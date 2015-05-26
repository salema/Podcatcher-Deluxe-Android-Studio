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

package com.podcatcher.deluxe.listeners;

import com.podcatcher.deluxe.model.types.Episode;

/**
 * Interface definition for a listener to be alerted when the old/new state of
 * an episode changes.
 */
public interface OnChangeEpisodeStateListener {

    /**
     * Called on the listener to indicate that the state (old/new) of an episode
     * has been altered.
     *
     * @param episode  Episode the state was changed for.
     * @param newState The new state the episode is in, i.e. <code>true</code>
     *                 if the episode is considered 'old' and <code>false</code>
     *                 otherwise.
     */
    void onStateChanged(Episode episode, boolean newState);

    /**
     * Called on the listener when the resume at point for an episode is
     * changed.
     *
     * @param episode Episode the metadata was changed for.
     * @param millis  The player position in milli-seconds from the start,
     *                <code>null</code> if the data has been reset.
     */
    void onResumeAtChanged(Episode episode, Integer millis);
}
