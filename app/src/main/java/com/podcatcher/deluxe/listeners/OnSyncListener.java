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


/**
 * Interface definition for a callback to be alerted about sync operations.
 */
public interface OnSyncListener {

    /**
     * Called on a call-back to indicate that the sync configuration
     * has changed, e.g. a {@link com.podcatcher.deluxe.model.sync.SyncController}
     * was activated or disabled.
     */
    void onSyncConfigChanged();

    /**
     * Called on a call-back to indicate that a sync action started.
     */
    void onSyncStarted();

    /**
     * Called on a call-back when all sync actions are finished.
     */
    void onSyncCompleted();
}
