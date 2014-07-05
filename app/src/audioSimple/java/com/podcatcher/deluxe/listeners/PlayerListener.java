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

package com.podcatcher.deluxe.listeners;

import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Interface definition for a callback to be by the player fragment.
 */
public interface PlayerListener extends OnSeekBarChangeListener {

    /**
     * Load/unload of the current episode requested.
     */
    public void onToggleLoad();

    /**
     * Play/pause of the current episode requested.
     */
    public void onTogglePlay();

    /**
     * Rewind of the current episode requested.
     */
    public void onRewind();

    /**
     * Fast-forward of the current episode requested.
     */
    public void onFastForward();

    /**
     * Alert the listener that it should return to the currently played episode.
     */
    public void onReturnToPlayingEpisode();
}
