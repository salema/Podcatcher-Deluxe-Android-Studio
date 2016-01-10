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
package com.podcatcher.labs.sync.podcare.callbacks;

import com.podcatcher.labs.sync.podcare.PodcareException;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Call-back for the Podcare connect action.
 */
public interface OnConnectListener {

    /**
     * Called on the listener if the connection was successfully established.
     *
     * @param connectId Id returned by the server. Clients will need to
     *                  store the connect id and present it for each
     *                  subsequent query to Podcare.
     */
    void onConnect(@NonNull String connectId);

    /**
     * Called on the listener if the connection was refused by the Podcare service.
     *
     * @param problem Exception thrown.
     */
    void onConnectFailed(@Nullable PodcareException problem);
}
