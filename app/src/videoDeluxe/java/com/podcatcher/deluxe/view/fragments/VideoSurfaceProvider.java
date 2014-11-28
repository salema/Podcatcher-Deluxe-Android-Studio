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

package com.podcatcher.deluxe.view.fragments;

import android.view.SurfaceHolder;

import com.podcatcher.deluxe.services.PlayEpisodeService;

/**
 * Listener interface to implement by fragments interested in showing video.
 *
 * @see PlayEpisodeService#setVideoSurfaceProvider(VideoSurfaceProvider)
 */
public interface VideoSurfaceProvider {

    /**
     * @return The surface holder for the video view. This will be used by the
     * episode playback service to show the video content when playing.
     */
    public SurfaceHolder getVideoSurface();

    /**
     * @return Whether the surface is available for playback. Needs to return
     * <code>true</code> if the surface is already available, and
     * <code>false</code> otherwise.
     */
    public boolean isVideoSurfaceAvailable();

    /**
     * Alert UI about the size of the video currently played back.
     *
     * @param width  The current video's width.
     * @param height The current video's height
     */
    public void adjustToVideoSize(int width, int height);
}
