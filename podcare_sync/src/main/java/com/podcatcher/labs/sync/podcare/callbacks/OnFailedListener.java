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

import android.support.annotation.Nullable;

/**
 * Call-back for Podcare actions that do not have a reply, but might fail.
 */
public interface OnFailedListener {

    /**
     * Called on the listener if the Podcare service performed the requested action.
     */
    void onSuccess();

    /**
     * Called on the listener if the Podcare service failed to perform the requested action.
     *
     * @param problem Exception thrown.
     */
    void onRequestFailed(@Nullable PodcareException problem);
}
