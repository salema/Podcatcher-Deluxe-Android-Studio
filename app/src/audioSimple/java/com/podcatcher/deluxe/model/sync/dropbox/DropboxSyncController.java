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

package com.podcatcher.deluxe.model.sync.dropbox;

import com.podcatcher.deluxe.model.sync.ControllerImpl;

import android.content.Context;

/**
 * A sync controller for the Dropbox service.
 */
public class DropboxSyncController extends DropboxPodcastListSyncController {

    /**
     * Create new sync controller for the Dropbox service.
     *
     * @param context The context to read the configuration from, in particular
     *                our {@link #ACCESS_TOKEN}. The controller will not function
     *                unless this is set correctly.
     */
    public DropboxSyncController(Context context) {
        super(context);
    }

    @Override
    public ControllerImpl getImpl() {
        return ControllerImpl.DROPBOX;
    }

    @Override
    public void syncSettings() {
        // pass, settings are not yet supported by this sync controller
    }
}
