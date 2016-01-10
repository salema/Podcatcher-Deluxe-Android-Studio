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
import com.podcatcher.deluxe.model.sync.SyncController;

import android.content.Context;

/**
 * A sync controller for the Dropbox service.
 */
public class DropboxSyncController extends DropboxEpisodeMetadataSyncController {

    /**
     * Create a {@link SyncController} for the Dropbox service.
     *
     * @param context Context we live in.
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
