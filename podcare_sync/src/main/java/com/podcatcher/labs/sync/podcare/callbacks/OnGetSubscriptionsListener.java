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
package com.podcatcher.labs.sync.podcare.callbacks;

import com.podcatcher.labs.sync.podcare.PodcareException;
import com.podcatcher.labs.sync.podcare.types.Subscription;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Call-back for the Podcare subscriptions action.
 */
public interface OnGetSubscriptionsListener {

    /**
     * Called on the listener if a list of podcasts was successfully received.
     *
     * @param feeds The list of podcasts, might be empty.
     */
    void onGetSubscriptions(List<Subscription> feeds);

    /**
     * Called on the listener if the podcast list was not received.
     *
     * @param problem Exception thrown.
     */
    void onGetSubscriptionsFailed(@Nullable PodcareException problem);
}
