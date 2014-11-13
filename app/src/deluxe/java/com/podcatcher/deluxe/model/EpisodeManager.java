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

package com.podcatcher.deluxe.model;

import com.podcatcher.deluxe.Podcatcher;

/**
 * Manager to handle episode specific activities. This is the bottom end of the
 * episode manager stack and finally provides access to the singleton object of
 * the manager.
 *
 * @see EpisodeBaseManager
 * @see EpisodeDownloadManager
 * @see EpisodePlaylistManager
 * @see EpisodeStateManager
 */
public class EpisodeManager extends EpisodeStateManager {

    /**
     * The single instance
     */
    private static EpisodeManager manager;

    /**
     * Init the episode manager.
     *
     * @param app The podcatcher application object (also a singleton).
     */
    private EpisodeManager(Podcatcher app) {
        super(app);
    }

    /**
     * Get the singleton instance of the episode manager.
     *
     * @param podcatcher Application handle.
     * @return The singleton instance.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static EpisodeManager getInstance(Podcatcher podcatcher) {
        // If not done, create single instance
        if (manager == null)
            manager = new EpisodeManager(podcatcher);

        return manager;
    }

    /**
     * Get the singleton instance of the podcast manager.
     *
     * @return The singleton instance.
     */
    public static EpisodeManager getInstance() {
        // In Application.onCreate() we make sure that this method is not called
        // unless the other one (with the application instance actually set) ran
        // to least once
        return manager;
    }
}
