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

package com.podcatcher.deluxe.model.sync.podcare;

import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.types.Episode;

import android.content.Context;

/**
 * A sync controller for the Podcare service.
 */
public class PodcareSyncController extends PodcareEpisodeMetadataSyncController {

    /**
     * Create new sync controller for the Podcare service.
     *
     * @param context The context to read the configuration from,
     *                in particular the {@link #CONNECT_ID_KEY}
     */
    public PodcareSyncController(Context context) {
        super(context);
    }

    @Override
    public ControllerImpl getImpl() {
        return ControllerImpl.PODCARE;
    }

    @Override
    public void syncSettings() {
        // pass, settings are not yet supported by this sync controller
    }

    @Override
    public void onDownloadSuccess(Episode episode) {
        // Podcare does not care for downloads
    }

    @Override
    public void onDownloadDeleted(Episode episode) {
        // Podcare does not care for downloads
    }
}
