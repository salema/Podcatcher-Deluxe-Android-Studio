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

package com.podcatcher.deluxe.model.sync.dropbox;

import android.content.Context;

import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.sync.SyncController;

/**
 * A sync controller for the Dropbox service.
 */
public class DropboxSyncController extends DropboxPodcastListSyncController {

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
    public boolean isRunning() {
        return super.isRunning();
    }
}
